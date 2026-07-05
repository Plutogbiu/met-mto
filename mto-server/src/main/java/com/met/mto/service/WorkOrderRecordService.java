package com.met.mto.service;

import com.met.mto.dto.WorkOrderRecordResponse;
import java.math.BigDecimal;
import java.util.List;

public interface WorkOrderRecordService {

    List<WorkOrderRecordResponse> list(Long workOrderId, Long currentUserId, String currentRole);

    WorkOrderRecordResponse createProcessRecord(
            Long workOrderId,
            String content,
            BigDecimal longitude,
            BigDecimal latitude,
            String locationAddress,
            Long operatorId,
            String operatorName,
            String currentRole
    );
}
