package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.met.mto.dto.FileAttachmentQuery;
import com.met.mto.dto.FileAttachmentResponse;
import com.met.mto.dto.WorkOrderRecordResponse;
import com.met.mto.entity.WorkOrder;
import com.met.mto.entity.WorkOrderRecord;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.WorkOrderMapper;
import com.met.mto.mapper.WorkOrderRecordMapper;
import com.met.mto.service.FileAttachmentService;
import com.met.mto.service.WorkOrderService;
import com.met.mto.service.WorkOrderRecordService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WorkOrderRecordServiceImpl implements WorkOrderRecordService {

    private final WorkOrderRecordMapper workOrderRecordMapper;
    private final WorkOrderMapper workOrderMapper;
    private final FileAttachmentService fileAttachmentService;
    private final WorkOrderService workOrderService;

    @Override
    public List<WorkOrderRecordResponse> list(Long workOrderId, Long currentUserId, String currentRole) {
        checkWorkOrder(workOrderId);
        workOrderService.checkAccess(workOrderId, currentUserId, currentRole);
        return workOrderRecordMapper.selectList(new LambdaQueryWrapper<WorkOrderRecord>()
                        .eq(WorkOrderRecord::getWorkOrderId, workOrderId)
                        .orderByDesc(WorkOrderRecord::getCreatedAt)
                        .orderByDesc(WorkOrderRecord::getId))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WorkOrderRecordResponse createProcessRecord(
            Long workOrderId,
            String content,
            BigDecimal longitude,
            BigDecimal latitude,
            String locationAddress,
            Long operatorId,
            String operatorName,
            String currentRole
    ) {
        checkWorkOrder(workOrderId);
        workOrderService.checkAccess(workOrderId, operatorId, currentRole);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写处理说明");
        }

        WorkOrderRecord record = new WorkOrderRecord();
        record.setWorkOrderId(workOrderId);
        record.setRecordType("process");
        record.setContent(content.trim());
        record.setLongitude(longitude);
        record.setLatitude(latitude);
        record.setLocationAddress(locationAddress);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setCreatedAt(LocalDateTime.now());
        workOrderRecordMapper.insert(record);
        return toResponse(record);
    }

    private void checkWorkOrder(Long workOrderId) {
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
    }

    private WorkOrderRecordResponse toResponse(WorkOrderRecord record) {
        FileAttachmentQuery query = new FileAttachmentQuery();
        query.setBizType("work_order_record");
        query.setBizId(record.getId());
        List<FileAttachmentResponse> attachments = fileAttachmentService.list(query);

        WorkOrderRecordResponse response = new WorkOrderRecordResponse();
        response.setId(record.getId());
        response.setWorkOrderId(record.getWorkOrderId());
        response.setRecordType(record.getRecordType());
        response.setStatusBefore(record.getStatusBefore());
        response.setStatusAfter(record.getStatusAfter());
        response.setContent(record.getContent());
        response.setLongitude(record.getLongitude());
        response.setLatitude(record.getLatitude());
        response.setLocationAddress(record.getLocationAddress());
        response.setOperatorId(record.getOperatorId());
        response.setOperatorName(record.getOperatorName());
        response.setCreatedAt(record.getCreatedAt());
        response.setAttachments(attachments);
        return response;
    }
}
