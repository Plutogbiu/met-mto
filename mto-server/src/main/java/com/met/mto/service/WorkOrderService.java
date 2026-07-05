package com.met.mto.service;

import com.met.mto.common.PageResult;
import com.met.mto.dto.WorkOrderQuery;
import com.met.mto.dto.WorkOrderRequest;
import com.met.mto.dto.WorkOrderResponse;

public interface WorkOrderService {

    PageResult<WorkOrderResponse> page(WorkOrderQuery query);

    WorkOrderResponse get(Long id);

    WorkOrderResponse create(WorkOrderRequest request, Long operatorId, String operatorName);

    WorkOrderResponse update(Long id, WorkOrderRequest request, Long operatorId, String operatorName);

    void updateStatus(Long id, String status, Long operatorId, String operatorName);

    void complete(Long id, Long operatorId, String operatorName);

    void checkAccess(Long id, Long currentUserId, String currentRole);
}
