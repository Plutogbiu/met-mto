package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.met.mto.dto.FileAttachmentQuery;
import com.met.mto.dto.FileAttachmentResponse;
import com.met.mto.entity.FileAttachment;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.FileAttachmentMapper;
import com.met.mto.service.FileAttachmentService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileAttachmentServiceImpl implements FileAttachmentService {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileAttachmentMapper fileAttachmentMapper;

    @Value("${mto.storage.local-path}")
    private String localStoragePath;

    @Override
    @Transactional
    public FileAttachmentResponse upload(MultipartFile file, String bizType, Long bizId, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? StringUtils.cleanPath(file.getOriginalFilename())
                : "file";
        String extension = StringUtils.getFilenameExtension(originalName);
        String fileName = UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.hasText(extension)) {
            fileName = fileName + "." + extension.toLowerCase();
        }

        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        Path relativePath = Paths.get(datePath, fileName);
        Path targetPath = Paths.get(localStoragePath).resolve(relativePath).normalize();

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        FileAttachment attachment = new FileAttachment();
        attachment.setBizType(StringUtils.hasText(bizType) ? bizType.trim() : null);
        attachment.setBizId(bizId);
        attachment.setCategory(StringUtils.hasText(category) ? category.trim() : null);
        attachment.setOriginalName(originalName);
        attachment.setFileName(fileName);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setStoragePath(relativePath.toString().replace("\\", "/"));
        attachment.setAccessUrl("/uploads/" + attachment.getStoragePath());
        attachment.setStatus(1);
        attachment.setCreatedAt(LocalDateTime.now());
        attachment.setUpdatedAt(attachment.getCreatedAt());
        fileAttachmentMapper.insert(attachment);
        return toResponse(attachment);
    }

    @Override
    public List<FileAttachmentResponse> list(FileAttachmentQuery query) {
        return fileAttachmentMapper.selectList(new LambdaQueryWrapper<FileAttachment>()
                        .eq(StringUtils.hasText(query.getBizType()), FileAttachment::getBizType, query.getBizType())
                        .eq(query.getBizId() != null, FileAttachment::getBizId, query.getBizId())
                        .eq(StringUtils.hasText(query.getCategory()), FileAttachment::getCategory, query.getCategory())
                        .eq(FileAttachment::getStatus, 1)
                        .orderByDesc(FileAttachment::getCreatedAt)
                        .orderByDesc(FileAttachment::getId))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FileAttachmentResponse get(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FileAttachment attachment = findById(id);
        attachment.setStatus(0);
        attachment.setUpdatedAt(LocalDateTime.now());
        fileAttachmentMapper.updateById(attachment);
        try {
            Files.deleteIfExists(Paths.get(localStoragePath).resolve(attachment.getStoragePath()).normalize());
        } catch (IOException ignored) {
            // Database record deletion has priority; physical cleanup failure does not block the action.
        }
    }

    private FileAttachment findById(Long id) {
        FileAttachment attachment = fileAttachmentMapper.selectById(id);
        if (attachment == null || attachment.getStatus() == null || attachment.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return attachment;
    }

    private FileAttachmentResponse toResponse(FileAttachment attachment) {
        FileAttachmentResponse response = new FileAttachmentResponse();
        response.setId(attachment.getId());
        response.setBizType(attachment.getBizType());
        response.setBizId(attachment.getBizId());
        response.setCategory(attachment.getCategory());
        response.setOriginalName(attachment.getOriginalName());
        response.setFileName(attachment.getFileName());
        response.setContentType(attachment.getContentType());
        response.setFileSize(attachment.getFileSize());
        response.setUrl(attachment.getAccessUrl());
        response.setCreatedAt(attachment.getCreatedAt());
        return response;
    }
}
