package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.met.mto.common.PageResult;
import com.met.mto.dto.CustomerDeviceQuery;
import com.met.mto.dto.CustomerDeviceRequest;
import com.met.mto.entity.CustomerDevice;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.CustomerDeviceMapper;
import com.met.mto.service.CustomerDeviceService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerDeviceServiceImpl implements CustomerDeviceService {

    private final CustomerDeviceMapper customerDeviceMapper;

    @Override
    public PageResult<CustomerDevice> page(CustomerDeviceQuery query) {
        String keyword = query.getKeyword();
        LambdaQueryWrapper<CustomerDevice> wrapper = new LambdaQueryWrapper<CustomerDevice>()
                .and(StringUtils.hasText(keyword), item -> item
                        .like(CustomerDevice::getName, keyword)
                        .or()
                        .like(CustomerDevice::getModel, keyword)
                        .or()
                        .like(CustomerDevice::getSerialNo, keyword))
                .eq(query.getStatus() != null, CustomerDevice::getStatus, query.getStatus())
                .orderByDesc(CustomerDevice::getUpdatedAt)
                .orderByDesc(CustomerDevice::getId);

        Page<CustomerDevice> page = customerDeviceMapper.selectPage(new Page<>(query.safePage(), query.safeSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CustomerDevice get(Long id) {
        return findById(id);
    }

    @Override
    @Transactional
    public CustomerDevice create(CustomerDeviceRequest request) {
        checkRequest(request);
        checkName(request.getName());
        CustomerDevice device = new CustomerDevice();
        apply(device, request);
        device.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(device.getCreatedAt());
        customerDeviceMapper.insert(device);
        return device;
    }

    @Override
    @Transactional
    public CustomerDevice update(Long id, CustomerDeviceRequest request) {
        checkRequest(request);
        checkName(request.getName());
        CustomerDevice device = findById(id);
        apply(device, request);
        if (request.getStatus() != null) {
            device.setStatus(request.getStatus());
        }
        device.setUpdatedAt(LocalDateTime.now());
        customerDeviceMapper.updateById(device);
        return device;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        CustomerDevice device = findById(id);
        device.setStatus(status == null ? 1 : status);
        device.setUpdatedAt(LocalDateTime.now());
        customerDeviceMapper.updateById(device);
    }

    private void apply(CustomerDevice device, CustomerDeviceRequest request) {
        device.setName(request.getName().trim());
        device.setModel(request.getModel());
        device.setSerialNo(request.getSerialNo());
    }

    private CustomerDevice findById(Long id) {
        CustomerDevice device = customerDeviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_DEVICE_NOT_FOUND);
        }
        return device;
    }

    private void checkRequest(CustomerDeviceRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private void checkName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.CUSTOMER_DEVICE_NAME_REQUIRED);
        }
    }
}
