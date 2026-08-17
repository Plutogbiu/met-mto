package com.met.mto.service;

import com.met.mto.dto.WorkOrderExportDownloadFile;
import com.met.mto.dto.WorkOrderExportDownloadTicketResponse;
import com.met.mto.dto.WorkOrderExportTaskRequest;
import com.met.mto.dto.WorkOrderExportTaskResponse;

public interface WorkOrderExportTaskService {

    WorkOrderExportTaskResponse create(WorkOrderExportTaskRequest request, Long creatorId, String creatorName);

    WorkOrderExportTaskResponse get(Long taskId, Long currentUserId, String currentRole);

    WorkOrderExportDownloadFile getDownloadFile(Long taskId, Long currentUserId, String currentRole);

    WorkOrderExportDownloadTicketResponse createDownloadTicket(Long taskId, Long currentUserId, String currentRole);

    WorkOrderExportDownloadFile consumeDownloadTicket(Long taskId, String ticket);
}
