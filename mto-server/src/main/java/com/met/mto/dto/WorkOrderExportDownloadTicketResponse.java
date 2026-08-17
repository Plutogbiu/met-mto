package com.met.mto.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkOrderExportDownloadTicketResponse {

    private String downloadUrl;
    private LocalDateTime expireAt;
}
