package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.common.PageResult;
import com.met.mto.dto.CustomerDeviceQuery;
import com.met.mto.dto.CustomerDeviceRequest;
import com.met.mto.entity.CustomerDevice;
import com.met.mto.security.PermissionCode;
import com.met.mto.security.RequirePermission;
import com.met.mto.service.CustomerDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class CustomerDeviceController {

    private final CustomerDeviceService customerDeviceService;

    @GetMapping
    @RequirePermission(PermissionCode.DEVICE_LIST)
    public ApiResult<PageResult<CustomerDevice>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        CustomerDeviceQuery query = new CustomerDeviceQuery();
        query.setKeyword(keyword);
        query.setStatus(status);
        query.setPage(page);
        query.setSize(size);
        return ApiResult.ok(customerDeviceService.page(query));
    }

    @GetMapping("/{id}")
    @RequirePermission(PermissionCode.DEVICE_DETAIL)
    public ApiResult<CustomerDevice> get(@PathVariable Long id) {
        return ApiResult.ok(customerDeviceService.get(id));
    }

    @PostMapping
    @RequirePermission(PermissionCode.DEVICE_CREATE)
    public ApiResult<CustomerDevice> create(@RequestBody CustomerDeviceRequest request) {
        return ApiResult.ok(customerDeviceService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission(PermissionCode.DEVICE_EDIT)
    public ApiResult<CustomerDevice> update(@PathVariable Long id, @RequestBody CustomerDeviceRequest request) {
        return ApiResult.ok(customerDeviceService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequirePermission(PermissionCode.DEVICE_STATUS)
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        customerDeviceService.updateStatus(id, status);
        return ApiResult.ok();
    }
}
