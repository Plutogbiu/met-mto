package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.met.mto.dto.WorkOrderExportDownloadFile;
import com.met.mto.dto.WorkOrderExportTaskRequest;
import com.met.mto.dto.WorkOrderExportTaskResponse;
import com.met.mto.entity.WorkOrder;
import com.met.mto.entity.WorkOrderExportItem;
import com.met.mto.entity.WorkOrderExportTask;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.WorkOrderExportItemMapper;
import com.met.mto.mapper.WorkOrderExportTaskMapper;
import com.met.mto.mapper.WorkOrderMapper;
import com.met.mto.service.WorkOrderExportTaskService;
import com.met.mto.service.WorkOrderReceiptPdfService;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkOrderExportTaskServiceImpl implements WorkOrderExportTaskService {

    private static final int DEFAULT_MAX_WORK_ORDER_COUNT = 200;
    private static final DateTimeFormatter TASK_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter ZIP_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderExportTaskMapper workOrderExportTaskMapper;
    private final WorkOrderExportItemMapper workOrderExportItemMapper;
    private final WorkOrderReceiptPdfService workOrderReceiptPdfService;

    private final Executor workOrderExportExecutor;

    @Value("${mto.export.local-path:./exports}")
    private String exportLocalPath;

    @Value("${mto.export.max-work-orders:200}")
    private int maxWorkOrderCount;

    @Value("${mto.export.expire-hours:24}")
    private long expireHours;

    public WorkOrderExportTaskServiceImpl(
            WorkOrderMapper workOrderMapper,
            WorkOrderExportTaskMapper workOrderExportTaskMapper,
            WorkOrderExportItemMapper workOrderExportItemMapper,
            WorkOrderReceiptPdfService workOrderReceiptPdfService,
            @Qualifier("workOrderExportExecutor") Executor workOrderExportExecutor
    ) {
        this.workOrderMapper = workOrderMapper;
        this.workOrderExportTaskMapper = workOrderExportTaskMapper;
        this.workOrderExportItemMapper = workOrderExportItemMapper;
        this.workOrderReceiptPdfService = workOrderReceiptPdfService;
        this.workOrderExportExecutor = workOrderExportExecutor;
    }

    @Override
    @Transactional
    public WorkOrderExportTaskResponse create(WorkOrderExportTaskRequest request, Long creatorId, String creatorName) {
        List<WorkOrder> orders = resolveOrders(request);
        int maxCount = maxExportCount();
        if (orders.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未找到可导出的已完成工单");
        }
        if (orders.size() > maxCount) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "单次最多导出 " + maxCount + " 条工单，请缩小筛选范围");
        }

        LocalDateTime now = LocalDateTime.now();
        WorkOrderExportTask task = new WorkOrderExportTask();
        task.setTaskNo(generateTaskNo());
        task.setCreatorId(creatorId);
        task.setCreatorName(StringUtils.hasText(creatorName) ? creatorName : "-");
        task.setStatus("pending");
        task.setTotalCount(orders.size());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        workOrderExportTaskMapper.insert(task);

        for (WorkOrder order : orders) {
            WorkOrderExportItem item = new WorkOrderExportItem();
            item.setTaskId(task.getId());
            item.setWorkOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setStatus("pending");
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            workOrderExportItemMapper.insert(item);
        }

        scheduleAfterCommit(task.getId());
        return toResponse(task);
    }

    @Override
    public WorkOrderExportTaskResponse get(Long taskId, Long currentUserId, String currentRole) {
        WorkOrderExportTask task = findTask(taskId);
        checkTaskAccess(task, currentUserId, currentRole);
        return toResponse(task);
    }

    @Override
    public WorkOrderExportDownloadFile getDownloadFile(Long taskId, Long currentUserId, String currentRole) {
        WorkOrderExportTask task = findTask(taskId);
        checkTaskAccess(task, currentUserId, currentRole);
        if (!"success".equals(task.getStatus()) || !StringUtils.hasText(task.getStoragePath())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "导出任务尚未完成");
        }
        if (task.getExpireAt() != null && !task.getExpireAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "导出文件已过期，请重新生成");
        }
        Path file = resolveExportFile(task.getStoragePath());
        if (!Files.isRegularFile(file)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "导出文件不存在或已清理");
        }
        try {
            return new WorkOrderExportDownloadFile(task.getFileName(), file, Files.size(file));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取导出文件失败");
        }
    }

    @PostConstruct
    public void resumePendingTasks() {
        List<WorkOrderExportTask> tasks = workOrderExportTaskMapper.selectList(new LambdaQueryWrapper<WorkOrderExportTask>()
                .eq(WorkOrderExportTask::getStatus, "pending")
                .orderByAsc(WorkOrderExportTask::getId));
        for (WorkOrderExportTask task : tasks) {
            submit(task.getId());
        }
        List<WorkOrderExportTask> processingTasks = workOrderExportTaskMapper.selectList(new LambdaQueryWrapper<WorkOrderExportTask>()
                .eq(WorkOrderExportTask::getStatus, "processing"));
        for (WorkOrderExportTask task : processingTasks) {
            failTask(task, "服务重启导致导出中断，请重新导出");
        }
    }

    @Scheduled(fixedDelay = 3600000L)
    public void cleanupExpiredFiles() {
        List<WorkOrderExportTask> tasks = workOrderExportTaskMapper.selectList(new LambdaQueryWrapper<WorkOrderExportTask>()
                .eq(WorkOrderExportTask::getStatus, "success")
                .le(WorkOrderExportTask::getExpireAt, LocalDateTime.now()));
        for (WorkOrderExportTask task : tasks) {
            if (StringUtils.hasText(task.getStoragePath())) {
                try {
                    Files.deleteIfExists(resolveExportFile(task.getStoragePath()));
                } catch (Exception exception) {
                    log.warn("清理导出文件失败：taskId={}", task.getId(), exception);
                    continue;
                }
            }
            task.setStatus("expired");
            task.setStoragePath(null);
            task.setUpdatedAt(LocalDateTime.now());
            workOrderExportTaskMapper.updateById(task);
        }
    }

    private void scheduleAfterCommit(Long taskId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    submit(taskId);
                }
            });
            return;
        }
        submit(taskId);
    }

    private void submit(Long taskId) {
        try {
            workOrderExportExecutor.execute(() -> process(taskId));
        } catch (TaskRejectedException exception) {
            WorkOrderExportTask task = workOrderExportTaskMapper.selectById(taskId);
            if (task != null && "pending".equals(task.getStatus())) {
                failTask(task, "导出任务队列已满，请稍后重试");
            }
        }
    }

    private void process(Long taskId) {
        WorkOrderExportTask task = workOrderExportTaskMapper.selectById(taskId);
        if (task == null || !"pending".equals(task.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus("processing");
        task.setStartedAt(now);
        task.setUpdatedAt(now);
        workOrderExportTaskMapper.updateById(task);

        Path tempDirectory = null;
        Path zipFile = null;
        int successCount = 0;
        int failedCount = 0;
        try {
            Path root = exportRoot();
            tempDirectory = root.resolve("temp").resolve(task.getTaskNo()).normalize();
            Files.createDirectories(tempDirectory);
            Path completedDirectory = root.resolve("completed").normalize();
            Files.createDirectories(completedDirectory);
            zipFile = completedDirectory.resolve(buildZipFileName(task)).normalize();

            List<WorkOrderExportItem> items = workOrderExportItemMapper.selectList(new LambdaQueryWrapper<WorkOrderExportItem>()
                    .eq(WorkOrderExportItem::getTaskId, taskId)
                    .orderByAsc(WorkOrderExportItem::getId));
            try (OutputStream output = Files.newOutputStream(zipFile, StandardOpenOption.CREATE_NEW);
                    ZipOutputStream zipOutput = new ZipOutputStream(output)) {
                for (WorkOrderExportItem item : items) {
                    try {
                        Path pdfFile = tempDirectory.resolve(item.getId() + ".pdf").normalize();
                        String fileName;
                        try (OutputStream pdfOutput = Files.newOutputStream(pdfFile, StandardOpenOption.CREATE_NEW)) {
                            fileName = workOrderReceiptPdfService.exportTo(item.getWorkOrderId(), pdfOutput);
                        }
                        zipOutput.putNextEntry(new ZipEntry(fileName));
                        Files.copy(pdfFile, zipOutput);
                        zipOutput.closeEntry();
                        Files.deleteIfExists(pdfFile);
                        succeedItem(item);
                        successCount++;
                    } catch (Exception exception) {
                        failItem(item, exception.getMessage());
                        failedCount++;
                    }
                    updateTaskProgress(taskId, successCount, failedCount);
                }
            }

            task = findTask(taskId);
            if (successCount <= 0) {
                Files.deleteIfExists(zipFile);
                failTask(task, "没有工单回执生成成功，请检查附件和 PDF 字体配置");
                return;
            }
            task.setStatus("success");
            task.setSuccessCount(successCount);
            task.setFailedCount(failedCount);
            task.setFileName(zipFile.getFileName().toString());
            task.setStoragePath(exportRoot().relativize(zipFile).toString().replace("\\", "/"));
            task.setFileSize(Files.size(zipFile));
            task.setCompletedAt(LocalDateTime.now());
            task.setExpireAt(task.getCompletedAt().plusHours(Math.max(expireHours, 1)));
            task.setUpdatedAt(task.getCompletedAt());
            workOrderExportTaskMapper.updateById(task);
        } catch (Exception exception) {
            log.error("工单批量导出失败：taskId={}", taskId, exception);
            WorkOrderExportTask latestTask = workOrderExportTaskMapper.selectById(taskId);
            if (latestTask != null) {
                failTask(latestTask, "批量导出失败：" + trimMessage(exception.getMessage()));
            }
            if (zipFile != null) {
                try {
                    Files.deleteIfExists(zipFile);
                } catch (IOException ignored) {
                    // 任务状态优先，临时文件由后续人工或系统清理。
                }
            }
        } finally {
            deleteDirectory(tempDirectory);
        }
    }

    private List<WorkOrder> resolveOrders(WorkOrderExportTaskRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (Boolean.TRUE.equals(request.getAllFiltered())) {
            return selectCompletedOrdersByFilter(request);
        }
        if (CollectionUtils.isEmpty(request.getWorkOrderIds())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择至少一条已完成工单");
        }
        List<Long> ids = new ArrayList<>();
        for (Long id : request.getWorkOrderIds()) {
            if (id != null && !ids.contains(id)) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择至少一条已完成工单");
        }
        if (ids.size() > maxExportCount()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "单次最多导出 " + maxExportCount() + " 条工单，请减少勾选数量");
        }
        List<WorkOrder> orders = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .in(WorkOrder::getId, ids)
                .eq(WorkOrder::getStatus, "completed")
                .orderByDesc(WorkOrder::getCompletedAt)
                .orderByDesc(WorkOrder::getId));
        if (orders.size() != ids.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只能导出已完成工单，请刷新列表后重试");
        }
        return orders;
    }

    private List<WorkOrder> selectCompletedOrdersByFilter(WorkOrderExportTaskRequest request) {
        LocalDateTime createdStart = parseStartTime(request.getCreatedStart());
        LocalDateTime createdEnd = parseEndTime(request.getCreatedEnd());
        LocalDateTime completedStart = parseStartTime(request.getCompletedStart());
        LocalDateTime completedEnd = parseEndTime(request.getCompletedEnd());
        String keyword = request.getKeyword();
        return workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .and(StringUtils.hasText(keyword), item -> item
                        .like(WorkOrder::getOrderNo, keyword)
                        .or()
                        .like(WorkOrder::getTitle, keyword)
                        .or()
                        .like(WorkOrder::getCustomerSiteName, keyword)
                        .or()
                        .like(WorkOrder::getDeviceName, keyword))
                .eq(StringUtils.hasText(request.getType()), WorkOrder::getType, request.getType())
                .eq(WorkOrder::getStatus, "completed")
                .ge(createdStart != null, WorkOrder::getCreatedAt, createdStart)
                .le(createdEnd != null, WorkOrder::getCreatedAt, createdEnd)
                .ge(completedStart != null, WorkOrder::getCompletedAt, completedStart)
                .le(completedEnd != null, WorkOrder::getCompletedAt, completedEnd)
                .orderByDesc(WorkOrder::getCompletedAt)
                .orderByDesc(WorkOrder::getId)
                .last("limit " + (maxExportCount() + 1)));
    }

    private LocalDateTime parseStartTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() == 10 ? LocalDate.parse(value).atStartOfDay() : LocalDateTime.parse(value);
    }

    private LocalDateTime parseEndTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() == 10 ? LocalDate.parse(value).atTime(23, 59, 59) : LocalDateTime.parse(value);
    }

    private void succeedItem(WorkOrderExportItem item) {
        item.setStatus("success");
        item.setErrorMessage(null);
        item.setUpdatedAt(LocalDateTime.now());
        workOrderExportItemMapper.updateById(item);
    }

    private void failItem(WorkOrderExportItem item, String message) {
        item.setStatus("failed");
        item.setErrorMessage(trimMessage(message));
        item.setUpdatedAt(LocalDateTime.now());
        workOrderExportItemMapper.updateById(item);
    }

    private void updateTaskProgress(Long taskId, int successCount, int failedCount) {
        WorkOrderExportTask task = workOrderExportTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setSuccessCount(successCount);
        task.setFailedCount(failedCount);
        task.setUpdatedAt(LocalDateTime.now());
        workOrderExportTaskMapper.updateById(task);
    }

    private void failTask(WorkOrderExportTask task, String message) {
        task.setStatus("failed");
        task.setErrorMessage(trimMessage(message));
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(task.getCompletedAt());
        workOrderExportTaskMapper.updateById(task);
    }

    private WorkOrderExportTask findTask(Long taskId) {
        WorkOrderExportTask task = workOrderExportTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "导出任务不存在");
        }
        return task;
    }

    private void checkTaskAccess(WorkOrderExportTask task, Long currentUserId, String currentRole) {
        if ("admin".equals(currentRole)) {
            return;
        }
        if (currentUserId == null || !currentUserId.equals(task.getCreatorId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Path exportRoot() throws IOException {
        Path root = Paths.get(exportLocalPath).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }

    private Path resolveExportFile(String storagePath) {
        try {
            Path root = exportRoot();
            Path file = root.resolve(storagePath).normalize();
            if (!file.startsWith(root)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "导出文件路径不正确");
            }
            return file;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出目录不可用");
        }
    }

    private String generateTaskNo() {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "WOE" + LocalDateTime.now().format(TASK_NO_FORMATTER) + random;
    }

    private int maxExportCount() {
        return maxWorkOrderCount <= 0 ? DEFAULT_MAX_WORK_ORDER_COUNT : maxWorkOrderCount;
    }

    private WorkOrderExportTaskResponse toResponse(WorkOrderExportTask task) {
        WorkOrderExportTaskResponse response = new WorkOrderExportTaskResponse();
        response.setId(task.getId());
        response.setTaskNo(task.getTaskNo());
        response.setStatus(task.getStatus());
        response.setTotalCount(task.getTotalCount());
        response.setSuccessCount(task.getSuccessCount());
        response.setFailedCount(task.getFailedCount());
        response.setFileName(task.getFileName());
        response.setFileSize(task.getFileSize());
        response.setErrorMessage(task.getErrorMessage());
        response.setStartedAt(task.getStartedAt());
        response.setCompletedAt(task.getCompletedAt());
        response.setExpireAt(task.getExpireAt());
        response.setCreatedAt(task.getCreatedAt());
        return response;
    }

    private String buildZipFileName(WorkOrderExportTask task) {
        return "物链易通-工单回执-" + LocalDateTime.now().format(ZIP_NAME_FORMATTER) + "_" + task.getTaskNo() + ".zip";
    }

    private String trimMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "生成失败，请查看服务端日志";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths
                    .sorted(Collections.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // 临时文件残留不会影响已完成 ZIP 的下载。
                        }
                    });
        } catch (IOException exception) {
            log.warn("清理导出临时目录失败：{}", directory, exception);
        }
    }
}
