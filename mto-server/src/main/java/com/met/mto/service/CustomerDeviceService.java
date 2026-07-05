package com.met.mto.service;

import com.met.mto.common.PageResult;
import com.met.mto.dto.CustomerDeviceQuery;
import com.met.mto.dto.CustomerDeviceRequest;
import com.met.mto.entity.CustomerDevice;

public interface CustomerDeviceService {

    PageResult<CustomerDevice> page(CustomerDeviceQuery query);

    CustomerDevice get(Long id);

    CustomerDevice create(CustomerDeviceRequest request);

    CustomerDevice update(Long id, CustomerDeviceRequest request);

    void updateStatus(Long id, Integer status);
}
