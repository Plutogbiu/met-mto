package com.met.mto.dto;

import lombok.Data;

@Data
public class FileAttachmentQuery {

    private String bizType;

    private Long bizId;

    private String category;
}
