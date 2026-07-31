package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.met.mto.dto.WorkOrderReceiptData;
import com.met.mto.dto.WorkOrderReceiptFile;
import com.met.mto.dto.WorkOrderReceiptImage;
import com.met.mto.entity.FileAttachment;
import com.met.mto.entity.SysUser;
import com.met.mto.entity.WorkOrder;
import com.met.mto.entity.WorkOrderEngineer;
import com.met.mto.entity.WorkOrderRecord;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.FileAttachmentMapper;
import com.met.mto.mapper.SysUserMapper;
import com.met.mto.mapper.WorkOrderEngineerMapper;
import com.met.mto.mapper.WorkOrderMapper;
import com.met.mto.mapper.WorkOrderRecordMapper;
import com.met.mto.service.WorkOrderReceiptPdfService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class WorkOrderReceiptPdfServiceImpl implements WorkOrderReceiptPdfService {

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PDF_IMAGE_CONTENT_TYPE = "image/jpeg";
    private static final Map<String, String> TYPE_LABELS = new HashMap<>();
    private static final Map<String, String> CATEGORY_LABELS = new HashMap<>();
    private static final Map<String, String> MAINTENANCE_CONTENT_LABELS = new HashMap<>();

    static {
        TYPE_LABELS.put("onsite", "现场工单");
        TYPE_LABELS.put("inspection", "日常巡检");
        CATEGORY_LABELS.put("fault_before", "原故障图片");
        CATEGORY_LABELS.put("fault_after", "处理后图片");
        CATEGORY_LABELS.put("site_process", "现场处理图片");
        CATEGORY_LABELS.put("work_receipt", "工单处理回执");
        CATEGORY_LABELS.put("inspection_photo", "巡检图片");
        CATEGORY_LABELS.put("inspection_receipt", "巡检处理回执");
        MAINTENANCE_CONTENT_LABELS.put("warranty_free", "保内免费");
        MAINTENANCE_CONTENT_LABELS.put("out_warranty_paid", "保外收费");
        MAINTENANCE_CONTENT_LABELS.put("out_warranty_free", "保外免费");
        MAINTENANCE_CONTENT_LABELS.put("warranty_paid", "保内收费");
    }

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderEngineerMapper workOrderEngineerMapper;
    private final WorkOrderRecordMapper workOrderRecordMapper;
    private final FileAttachmentMapper fileAttachmentMapper;
    private final SysUserMapper sysUserMapper;
    private final SpringTemplateEngine templateEngine;

    @Value("${mto.storage.local-path}")
    private String localStoragePath;

    @Value("${mto.pdf.font-path:}")
    private String fontPath;

    @Value("${mto.pdf.image-max-edge:1600}")
    private int pdfImageMaxEdge;

    @Value("${mto.pdf.image-quality:0.82}")
    private float pdfImageQuality;

    @Override
    public WorkOrderReceiptFile export(Long workOrderId) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            String fileName = exportTo(workOrderId, output);
            return new WorkOrderReceiptFile(fileName, output.toByteArray());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.REPORT_GENERATION_FAILED, "回执 PDF 生成失败");
        }
    }

    @Override
    public String exportTo(Long workOrderId, OutputStream output) {
        WorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!"completed".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已完成工单可以导出回执 PDF");
        }

        WorkOrderReceiptData data = buildData(order);
        Context context = new Context();
        context.setVariable("report", data);
        String html = templateEngine.process("receipt/work-order", context);

        try {
            PdfRendererBuilder builder = new PdfRendererBuilder()
                    .useFastMode()
                    .withHtmlContent(html, null)
                    .toStream(output);
            configureFont(builder);
            builder.run();
            return buildFileName(order);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.REPORT_GENERATION_FAILED, "回执 PDF 生成失败，请检查中文字体配置");
        }
    }

    private WorkOrderReceiptData buildData(WorkOrder order) {
        List<WorkOrderRecord> records = workOrderRecordMapper.selectList(new LambdaQueryWrapper<WorkOrderRecord>()
                .eq(WorkOrderRecord::getWorkOrderId, order.getId())
                .orderByAsc(WorkOrderRecord::getCreatedAt)
                .orderByAsc(WorkOrderRecord::getId));
        WorkOrderRecord createRecord = records.stream()
                .filter(record -> "create".equals(record.getRecordType()))
                .findFirst()
                .orElse(null);

        WorkOrderReceiptData data = new WorkOrderReceiptData();
        data.setCompanyLogoDataUri(loadCompanyLogoDataUri());
        data.setOrderNo(valueOrDash(order.getOrderNo()));
        data.setType(label(TYPE_LABELS, order.getType()));
        data.setCreatedAt(format(order.getCreatedAt()));
        data.setCompletedAt(format(order.getCompletedAt()));
        data.setCustomerName(valueOrDash(order.getCustomerSiteName()));
        data.setCustomerAddress(valueOrDash(order.getCustomerAddress()));
        data.setDeviceName(valueOrDash(order.getDeviceName()));
        data.setMaintenanceContent(label(MAINTENANCE_CONTENT_LABELS, order.getMaintenanceContent()));
        data.setContent(valueOrDash(order.getContent()));
        data.setCreatorName(resolveCreator(createRecord));
        data.setEngineers(loadEngineerNames(order.getId()));
        data.setImages(loadImages(order.getId()));
        data.setImageSectionTitle("inspection".equals(order.getType())
                ? "巡检图片和巡检处理回执"
                : "现场图片和回执图片");
        data.setGeneratedAt(format(LocalDateTime.now()));
        return data;
    }

    private String loadCompanyLogoDataUri() {
        ClassPathResource logo = new ClassPathResource("templates/assets/log.jpg");
        try (InputStream input = logo.getInputStream()) {
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(readAllBytes(input));
        } catch (IOException ignored) {
            return null;
        }
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = input.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }
        return output.toByteArray();
    }

    private List<String> loadEngineerNames(Long workOrderId) {
        return workOrderEngineerMapper.selectList(new LambdaQueryWrapper<WorkOrderEngineer>()
                        .eq(WorkOrderEngineer::getWorkOrderId, workOrderId)
                        .orderByAsc(WorkOrderEngineer::getId))
                .stream()
                .map(engineer -> StringUtils.hasText(engineer.getRealName())
                        ? engineer.getRealName()
                        : valueOrDash(engineer.getUsername()))
                .collect(Collectors.toList());
    }

    private List<WorkOrderReceiptImage> loadImages(Long workOrderId) {
        Path root = Paths.get(localStoragePath).toAbsolutePath().normalize();
        return fileAttachmentMapper.selectList(new LambdaQueryWrapper<FileAttachment>()
                        .eq(FileAttachment::getBizType, "work_order")
                        .eq(FileAttachment::getBizId, workOrderId)
                        .eq(FileAttachment::getStatus, 1)
                        .orderByAsc(FileAttachment::getCreatedAt)
                        .orderByAsc(FileAttachment::getId))
                .stream()
                .map(attachment -> toImage(attachment, root))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private WorkOrderReceiptImage toImage(FileAttachment attachment, Path root) {
        String contentType = attachment.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase().startsWith("image/")) {
            return null;
        }
        try {
            Path file = root.resolve(attachment.getStoragePath()).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return null;
            }
            String dataUri = createPdfImageDataUri(file);
            if (dataUri == null) {
                return null;
            }
            return new WorkOrderReceiptImage(
                    attachment.getCategory(),
                    label(CATEGORY_LABELS, attachment.getCategory()),
                    dataUri,
                    valueOrDash(attachment.getOriginalName()));
        } catch (IOException ignored) {
            return null;
        }
    }

    private String createPdfImageDataUri(Path file) throws IOException {
        BufferedImage source = ImageIO.read(file.toFile());
        if (source == null) {
            return null;
        }
        try {
            int maxEdge = Math.max(pdfImageMaxEdge, 320);
            int sourceWidth = source.getWidth();
            int sourceHeight = source.getHeight();
            double scale = Math.min(1D, maxEdge / (double) Math.max(sourceWidth, sourceHeight));
            int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
            int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));

            BufferedImage normalized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = normalized.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, targetWidth, targetHeight);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            } finally {
                graphics.dispose();
            }

            try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                    ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                if (!writers.hasNext()) {
                    throw new IOException("未找到 JPEG 图片编码器");
                }
                ImageWriter writer = writers.next();
                try {
                    writer.setOutput(imageOutput);
                    ImageWriteParam parameters = writer.getDefaultWriteParam();
                    if (parameters.canWriteCompressed()) {
                        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        parameters.setCompressionQuality(Math.max(0.1F, Math.min(pdfImageQuality, 1F)));
                    }
                    writer.write(null, new IIOImage(normalized, null, null), parameters);
                } finally {
                    writer.dispose();
                    normalized.flush();
                }
                return "data:" + PDF_IMAGE_CONTENT_TYPE + ";base64,"
                        + Base64.getEncoder().encodeToString(output.toByteArray());
            }
        } finally {
            source.flush();
        }
    }

    private String resolveCreator(WorkOrderRecord createRecord) {
        if (createRecord == null) {
            return "-";
        }
        if (createRecord.getOperatorId() != null) {
            SysUser user = sysUserMapper.selectById(createRecord.getOperatorId());
            if (user != null && StringUtils.hasText(user.getRealName())) {
                return user.getRealName();
            }
        }
        return valueOrDash(createRecord.getOperatorName());
    }

    private void configureFont(PdfRendererBuilder builder) {
        if (!StringUtils.hasText(fontPath)) {
            throw new BusinessException(ErrorCode.REPORT_GENERATION_FAILED, "未配置 PDF 中文字体路径");
        }
        Path font = Paths.get(fontPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(font)) {
            throw new BusinessException(ErrorCode.REPORT_GENERATION_FAILED,
                    "未找到 PDF 中文字体文件：" + font);
        }
        builder.useFont(font.toFile(), "MtoSans");
    }

    private String buildFileName(WorkOrder order) {
        String type = label(TYPE_LABELS, order.getType());
        String createdAt = order.getCreatedAt() == null ? "未知时间" : order.getCreatedAt().format(FILE_FORMATTER);
        String name = "物链易通-" + valueOrDash(order.getCustomerSiteName()) + "-" + type + "-" + createdAt + "_" + valueOrDash(order.getOrderNo());
        return name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".pdf";
    }

    private String format(LocalDateTime value) {
        return value == null ? "-" : value.format(DISPLAY_FORMATTER);
    }

    private String valueOrDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String label(Map<String, String> labels, String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        return labels.getOrDefault(value, value);
    }
}
