package com.met.mto.service;

import com.met.mto.dto.FileAttachmentQuery;
import com.met.mto.dto.FileAttachmentResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileAttachmentService {

    FileAttachmentResponse upload(MultipartFile file, String bizType, Long bizId, String category);

    List<FileAttachmentResponse> list(FileAttachmentQuery query);

    FileAttachmentResponse get(Long id);

    void delete(Long id);
}
