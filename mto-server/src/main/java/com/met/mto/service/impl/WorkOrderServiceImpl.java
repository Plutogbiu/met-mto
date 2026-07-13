package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.met.mto.common.PageResult;
import com.met.mto.dto.WorkOrderEngineerResponse;
import com.met.mto.dto.WorkOrderQuery;
import com.met.mto.dto.WorkOrderRequest;
import com.met.mto.dto.WorkOrderResponse;
import com.met.mto.entity.CustomerDevice;
import com.met.mto.entity.CustomerSite;
import com.met.mto.entity.FileAttachment;
import com.met.mto.entity.SysUser;
import com.met.mto.entity.WorkOrder;
import com.met.mto.entity.WorkOrderEngineer;
import com.met.mto.entity.WorkOrderRecord;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.CustomerDeviceMapper;
import com.met.mto.mapper.CustomerSiteMapper;
import com.met.mto.mapper.FileAttachmentMapper;
import com.met.mto.mapper.SysUserMapper;
import com.met.mto.mapper.WorkOrderEngineerMapper;
import com.met.mto.mapper.WorkOrderMapper;
import com.met.mto.mapper.WorkOrderRecordMapper;
import com.met.mto.service.WorkOrderService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("onsite", "inspection"));
    private static final Set<String> PRIORITIES = new HashSet<>(Arrays.asList("normal", "urgent"));
    private static final Set<String> STATUSES = new HashSet<>(Arrays.asList("pending", "processing", "completed", "closed"));
    private static final Set<String> UNFINISHED_STATUSES = new HashSet<>(Arrays.asList("pending", "processing"));

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderEngineerMapper workOrderEngineerMapper;
    private final WorkOrderRecordMapper workOrderRecordMapper;
    private final CustomerDeviceMapper customerDeviceMapper;
    private final CustomerSiteMapper customerSiteMapper;
    private final SysUserMapper sysUserMapper;
    private final FileAttachmentMapper fileAttachmentMapper;

    @Override
    public PageResult<WorkOrderResponse> page(WorkOrderQuery query) {
        List<Long> filteredOrderIds = findOrderIdsByEngineer(query.getEngineerId());
        if (query.getEngineerId() != null && filteredOrderIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0, query.safePage(), query.safeSize());
        }

        String keyword = query.getKeyword();
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<WorkOrder>()
                .and(StringUtils.hasText(keyword), item -> item
                        .like(WorkOrder::getOrderNo, keyword)
                        .or()
                        .like(WorkOrder::getTitle, keyword)
                        .or()
                        .like(WorkOrder::getCustomerSiteName, keyword)
                        .or()
                        .like(WorkOrder::getDeviceName, keyword))
                .eq(StringUtils.hasText(query.getType()), WorkOrder::getType, query.getType())
                .eq(StringUtils.hasText(query.getStatus()), WorkOrder::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getCustomerKeyword()), WorkOrder::getCustomerSiteName, query.getCustomerKeyword())
                .in(Boolean.TRUE.equals(query.getUnfinishedOnly()), WorkOrder::getStatus, UNFINISHED_STATUSES)
                .eq(query.getCustomerSiteId() != null, WorkOrder::getCustomerSiteId, query.getCustomerSiteId())
                .in(!filteredOrderIds.isEmpty(), WorkOrder::getId, filteredOrderIds)
                .ge(query.getCreatedStart() != null, WorkOrder::getCreatedAt, query.getCreatedStart())
                .le(query.getCreatedEnd() != null, WorkOrder::getCreatedAt, query.getCreatedEnd())
                .orderByDesc(WorkOrder::getUpdatedAt)
                .orderByDesc(WorkOrder::getId);

        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(query.safePage(), query.safeSize()), wrapper);
        Map<Long, List<WorkOrderEngineerResponse>> engineerMap = loadEngineerMap(page.getRecords().stream()
                .map(WorkOrder::getId)
                .collect(Collectors.toList()));
        List<WorkOrderResponse> records = page.getRecords().stream()
                .map(order -> toResponse(order, engineerMap.getOrDefault(order.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public WorkOrderResponse get(Long id) {
        WorkOrder order = findById(id);
        return toResponse(order, loadEngineers(id));
    }

    @Override
    @Transactional
    public WorkOrderResponse create(WorkOrderRequest request, Long operatorId, String operatorName) {
        checkRequest(request);
        CustomerSite customerSite = findCustomerSite(request.getCustomerSiteId());
        CustomerDevice device = findDevice(request);
        List<SysUser> engineers = findEngineers(request.getEngineerIds());

        WorkOrder order = new WorkOrder();
        order.setOrderNo(generateOrderNo());
        apply(order, request, customerSite, device);
        order.setStatus("pending");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(order.getCreatedAt());
        workOrderMapper.insert(order);
        saveEngineers(order.getId(), engineers);
        saveRecord(order.getId(), "create", null, order.getStatus(), "创建工单", operatorId, operatorName);
        return get(order.getId());
    }

    @Override
    @Transactional
    public WorkOrderResponse update(Long id, WorkOrderRequest request, Long operatorId, String operatorName) {
        checkRequest(request);
        WorkOrder order = findById(id);
        CustomerSite customerSite = findCustomerSite(request.getCustomerSiteId());
        CustomerDevice device = findDevice(request);
        List<SysUser> engineers = findEngineers(request.getEngineerIds());

        apply(order, request, customerSite, device);
        if (StringUtils.hasText(request.getStatus())) {
            checkStatus(request.getStatus());
            if ("closed".equals(request.getStatus()) && !"closed".equals(order.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请通过作废功能处理工单");
            }
            order.setStatus(request.getStatus());
            if ("completed".equals(request.getStatus()) && order.getCompletedAt() == null) {
                order.setCompletedAt(LocalDateTime.now());
            }
        }
        order.setUpdatedAt(LocalDateTime.now());
        workOrderMapper.updateById(order);
        replaceEngineers(order.getId(), engineers);
        saveRecord(order.getId(), "update", null, order.getStatus(), "更新工单信息", operatorId, operatorName);
        return get(order.getId());
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status, Long operatorId, String operatorName) {
        checkStatus(status);
        if ("closed".equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请通过作废功能处理工单");
        }
        changeStatus(id, status, "状态变更", operatorId, operatorName);
    }

    @Override
    @Transactional
    public void voidOrder(Long id, String reason, Long operatorId, String operatorName) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写作废原因");
        }
        WorkOrder order = findById(id);
        if ("closed".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工单已作废");
        }
        if ("completed".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已完成工单不能作废");
        }
        String oldStatus = order.getStatus();
        order.setStatus("closed");
        order.setUpdatedAt(LocalDateTime.now());
        workOrderMapper.updateById(order);
        saveRecord(id, "void", oldStatus, "closed", reason.trim(), operatorId, operatorName);
    }

    @Override
    @Transactional
    public void complete(Long id, Long operatorId, String operatorName) {
        checkCompleteRequirements(id);
        changeStatus(id, "completed", "完成工单", operatorId, operatorName);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WorkOrder order = findById(id);
        if ("completed".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已完成工单不能删除");
        }

        List<Long> recordIds = workOrderRecordMapper.selectList(new LambdaQueryWrapper<WorkOrderRecord>()
                        .eq(WorkOrderRecord::getWorkOrderId, id))
                .stream()
                .map(WorkOrderRecord::getId)
                .collect(Collectors.toList());

        workOrderEngineerMapper.delete(new LambdaQueryWrapper<WorkOrderEngineer>()
                .eq(WorkOrderEngineer::getWorkOrderId, id));

        fileAttachmentMapper.delete(new LambdaQueryWrapper<FileAttachment>()
                .eq(FileAttachment::getBizType, "work_order")
                .eq(FileAttachment::getBizId, id));

        if (!recordIds.isEmpty()) {
            fileAttachmentMapper.delete(new LambdaQueryWrapper<FileAttachment>()
                    .eq(FileAttachment::getBizType, "work_order_record")
                    .in(FileAttachment::getBizId, recordIds));
        }

        workOrderRecordMapper.delete(new LambdaQueryWrapper<WorkOrderRecord>()
                .eq(WorkOrderRecord::getWorkOrderId, id));
        workOrderMapper.deleteById(id);
    }

    @Override
    public void checkAccess(Long id, Long currentUserId, String currentRole) {
        if (!"field_engineer".equals(currentRole)) {
            return;
        }
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long count = workOrderEngineerMapper.selectCount(new LambdaQueryWrapper<WorkOrderEngineer>()
                .eq(WorkOrderEngineer::getWorkOrderId, id)
                .eq(WorkOrderEngineer::getUserId, currentUserId));
        if (count == null || count <= 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查看和处理指派给自己的工单");
        }
    }

    private void changeStatus(Long id, String status, String recordContent, Long operatorId, String operatorName) {
        WorkOrder order = findById(id);
        if ("closed".equals(order.getStatus()) && !"closed".equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已作废工单不能变更状态");
        }
        String oldStatus = order.getStatus();
        order.setStatus(status);
        if ("completed".equals(status) && order.getCompletedAt() == null) {
            order.setCompletedAt(LocalDateTime.now());
        }
        order.setUpdatedAt(LocalDateTime.now());
        workOrderMapper.updateById(order);
        saveRecord(id, "status", oldStatus, status, recordContent, operatorId, operatorName);
    }

    private void checkCompleteRequirements(Long workOrderId) {
        WorkOrder order = findById(workOrderId);
        if ("completed".equals(order.getStatus())) {
            return;
        }
        if ("closed".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已作废工单不能变更状态");
        }

        Long attachmentCount = fileAttachmentMapper.selectCount(new LambdaQueryWrapper<FileAttachment>()
                .eq(FileAttachment::getBizType, "work_order")
                .eq(FileAttachment::getBizId, workOrderId)
                .eq(FileAttachment::getStatus, 1));
        if (attachmentCount == null || attachmentCount <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少上传 1 张现场附件后再完成工单");
        }

        List<WorkOrderEngineer> engineers = workOrderEngineerMapper.selectList(new LambdaQueryWrapper<WorkOrderEngineer>()
                .eq(WorkOrderEngineer::getWorkOrderId, workOrderId)
                .orderByAsc(WorkOrderEngineer::getId));
        if (engineers.isEmpty()) {
            throw new BusinessException(ErrorCode.WORK_ORDER_ENGINEER_REQUIRED);
        }

        Set<Long> checkedInUserIds = workOrderRecordMapper.selectList(new LambdaQueryWrapper<WorkOrderRecord>()
                .eq(WorkOrderRecord::getWorkOrderId, workOrderId)
                .eq(WorkOrderRecord::getRecordType, "process")
                .isNotNull(WorkOrderRecord::getLongitude)
                .isNotNull(WorkOrderRecord::getLatitude))
                .stream()
                .map(WorkOrderRecord::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> missingNames = engineers.stream()
                .filter(engineer -> !checkedInUserIds.contains(engineer.getUserId()))
                .map(this::engineerDisplayName)
                .collect(Collectors.toList());
        if (!missingNames.isEmpty()) {
            throw new BusinessException(ErrorCode.WORK_ORDER_CHECKIN_REQUIRED,
                    "还有工程师未完成现场打卡：" + String.join("、", missingNames));
        }
    }

    private String engineerDisplayName(WorkOrderEngineer engineer) {
        if (StringUtils.hasText(engineer.getRealName())) {
            return engineer.getRealName();
        }
        if (StringUtils.hasText(engineer.getUsername())) {
            return engineer.getUsername();
        }
        return String.valueOf(engineer.getUserId());
    }

    private void apply(WorkOrder order, WorkOrderRequest request, CustomerSite customerSite, CustomerDevice device) {
        checkType(request.getType());
        checkPriority(request.getPriority());
        checkArrivalTime(request.getEstimatedArrivalTime());
        checkCompleteTime(request);
        order.setTitle(generateTitle(request.getType(), customerSite, device));
        order.setType(request.getType());
        order.setCustomerSiteId(customerSite.getId());
        order.setCustomerSiteName(customerSite.getName());
        order.setCustomerAddress(customerSite.getAddress());
        order.setDeviceId(device == null ? null : device.getId());
        order.setDeviceName(device == null ? null : device.getName());
        order.setPriority(StringUtils.hasText(request.getPriority()) ? request.getPriority() : "normal");
        order.setMaintenanceContent(request.getMaintenanceContent());
        order.setContent(request.getContent());
        order.setNotice(request.getNotice());
        order.setEstimatedArrivalTime(request.getEstimatedArrivalTime());
        order.setEstimatedCompleteTime("onsite".equals(request.getType()) ? request.getEstimatedCompleteTime() : null);
    }

    private WorkOrder findById(Long id) {
        WorkOrder order = workOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        return order;
    }

    private CustomerSite findCustomerSite(Long customerSiteId) {
        if (customerSiteId == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_CUSTOMER_REQUIRED);
        }
        CustomerSite customerSite = customerSiteMapper.selectById(customerSiteId);
        if (customerSite == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_SITE_NOT_FOUND);
        }
        return customerSite;
    }

    private CustomerDevice findDevice(WorkOrderRequest request) {
        if (!"onsite".equals(request.getType())) {
            return null;
        }
        if (request.getDeviceId() == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_DEVICE_REQUIRED);
        }
        CustomerDevice device = customerDeviceMapper.selectById(request.getDeviceId());
        if (device == null || device.getStatus() == null || device.getStatus() != 1) {
            throw new BusinessException(ErrorCode.WORK_ORDER_DEVICE_REQUIRED, "请选择已启用的设备");
        }
        return device;
    }

    private List<SysUser> findEngineers(List<Long> engineerIds) {
        if (CollectionUtils.isEmpty(engineerIds)) {
            throw new BusinessException(ErrorCode.WORK_ORDER_ENGINEER_REQUIRED);
        }
        List<Long> distinctIds = engineerIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            throw new BusinessException(ErrorCode.WORK_ORDER_ENGINEER_REQUIRED);
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(distinctIds);
        Set<Long> validIds = users.stream()
                .filter(user -> "field_engineer".equals(user.getRole()) && user.getStatus() != null && user.getStatus() == 1)
                .map(SysUser::getId)
                .collect(Collectors.toSet());
        if (validIds.size() != distinctIds.size()) {
            throw new BusinessException(ErrorCode.WORK_ORDER_ENGINEER_REQUIRED, "请选择已启用的现场实施工程师");
        }
        return users;
    }

    private void replaceEngineers(Long workOrderId, List<SysUser> engineers) {
        workOrderEngineerMapper.delete(new LambdaQueryWrapper<WorkOrderEngineer>()
                .eq(WorkOrderEngineer::getWorkOrderId, workOrderId));
        saveEngineers(workOrderId, engineers);
    }

    private void saveEngineers(Long workOrderId, List<SysUser> engineers) {
        LocalDateTime now = LocalDateTime.now();
        for (SysUser user : engineers) {
            WorkOrderEngineer engineer = new WorkOrderEngineer();
            engineer.setWorkOrderId(workOrderId);
            engineer.setUserId(user.getId());
            engineer.setUsername(user.getUsername());
            engineer.setRealName(user.getRealName());
            engineer.setCreatedAt(now);
            workOrderEngineerMapper.insert(engineer);
        }
    }

    private void saveRecord(Long workOrderId, String recordType, String statusBefore, String statusAfter, String content, Long operatorId, String operatorName) {
        WorkOrderRecord record = new WorkOrderRecord();
        record.setWorkOrderId(workOrderId);
        record.setRecordType(recordType);
        record.setStatusBefore(statusBefore);
        record.setStatusAfter(statusAfter);
        record.setContent(content);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setCreatedAt(LocalDateTime.now());
        workOrderRecordMapper.insert(record);
    }

    private List<WorkOrderEngineerResponse> loadEngineers(Long workOrderId) {
        return workOrderEngineerMapper.selectList(new LambdaQueryWrapper<WorkOrderEngineer>()
                        .eq(WorkOrderEngineer::getWorkOrderId, workOrderId)
                        .orderByAsc(WorkOrderEngineer::getId))
                .stream()
                .map(this::toEngineerResponse)
                .collect(Collectors.toList());
    }

    private Map<Long, List<WorkOrderEngineerResponse>> loadEngineerMap(List<Long> workOrderIds) {
        if (workOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return workOrderEngineerMapper.selectList(new LambdaQueryWrapper<WorkOrderEngineer>()
                        .in(WorkOrderEngineer::getWorkOrderId, workOrderIds)
                        .orderByAsc(WorkOrderEngineer::getId))
                .stream()
                .collect(Collectors.groupingBy(
                        WorkOrderEngineer::getWorkOrderId,
                        Collectors.mapping(this::toEngineerResponse, Collectors.toList())
                ));
    }

    private List<Long> findOrderIdsByEngineer(Long engineerId) {
        if (engineerId == null) {
            return new ArrayList<>();
        }
        return workOrderEngineerMapper.selectList(new LambdaQueryWrapper<WorkOrderEngineer>()
                        .eq(WorkOrderEngineer::getUserId, engineerId))
                .stream()
                .map(WorkOrderEngineer::getWorkOrderId)
                .distinct()
                .collect(Collectors.toList());
    }

    private void checkRequest(WorkOrderRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private void checkType(String type) {
        if (!StringUtils.hasText(type) || !TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工单类型不正确");
        }
    }

    private void checkPriority(String priority) {
        if (StringUtils.hasText(priority) && !PRIORITIES.contains(priority)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工单优先级不正确");
        }
    }

    private void checkStatus(String status) {
        if (!StringUtils.hasText(status) || !STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工单状态不正确");
        }
    }

    private void checkArrivalTime(LocalDateTime estimatedArrivalTime) {
        if (estimatedArrivalTime == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_ARRIVAL_TIME_REQUIRED);
        }
    }

    private void checkCompleteTime(WorkOrderRequest request) {
        if ("onsite".equals(request.getType())
                && request.getEstimatedCompleteTime() == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_COMPLETE_TIME_REQUIRED);
        }
    }

    private String generateOrderNo() {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "WO" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + random;
    }

    private String generateTitle(String type, CustomerSite customerSite, CustomerDevice device) {
        String typeName = "inspection".equals(type) ? "日常巡检" : "现场工单";
        if (device != null) {
            return customerSite.getName() + "-" + device.getName() + "-" + typeName;
        }
        return customerSite.getName() + "-" + typeName;
    }

    private WorkOrderResponse toResponse(WorkOrder order, List<WorkOrderEngineerResponse> engineers) {
        WorkOrderResponse response = new WorkOrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setTitle(order.getTitle());
        response.setType(order.getType());
        response.setCustomerSiteId(order.getCustomerSiteId());
        response.setCustomerSiteName(order.getCustomerSiteName());
        response.setCustomerAddress(order.getCustomerAddress());
        response.setDeviceId(order.getDeviceId());
        response.setDeviceName(order.getDeviceName());
        response.setPriority(order.getPriority());
        response.setStatus(order.getStatus());
        response.setMaintenanceContent(order.getMaintenanceContent());
        response.setContent(order.getContent());
        response.setNotice(order.getNotice());
        response.setEstimatedArrivalTime(order.getEstimatedArrivalTime());
        response.setEstimatedCompleteTime(order.getEstimatedCompleteTime());
        response.setCompletedAt(order.getCompletedAt());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setEngineers(engineers);
        return response;
    }

    private WorkOrderEngineerResponse toEngineerResponse(WorkOrderEngineer engineer) {
        WorkOrderEngineerResponse response = new WorkOrderEngineerResponse();
        response.setUserId(engineer.getUserId());
        response.setUsername(engineer.getUsername());
        response.setRealName(engineer.getRealName());
        return response;
    }
}
