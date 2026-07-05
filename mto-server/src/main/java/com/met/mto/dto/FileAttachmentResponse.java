package com.met.mto.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FileAttachmentResponse {

    private Long id;

    private String bizType;

    private Long bizId;

    private String category;

    private String originalName;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String url;

    private LocalDateTime createdAt;
}
