package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.common.PageResult;
import com.met.mto.dto.CustomerSiteQuery;
import com.met.mto.dto.CustomerSiteRequest;
import com.met.mto.entity.CustomerSite;
import com.met.mto.security.PermissionCode;
import com.met.mto.security.RequirePermission;
import com.met.mto.service.CustomerSiteService;
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
@RequestMapping("/api/admin/customer-sites")
@RequiredArgsConstructor
public class CustomerSiteController {

    private final CustomerSiteService customerSiteService;

    @GetMapping
    @RequirePermission(PermissionCode.CUSTOMER_LIST)
    public ApiResult<PageResult<CustomerSite>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        CustomerSiteQuery query = new CustomerSiteQuery();
        query.setKeyword(keyword);
        query.setStatus(status);
        query.setPage(page);
        query.setSize(size);
        return ApiResult.ok(customerSiteService.page(query));
    }

    @GetMapping("/{id}")
    @RequirePermission(PermissionCode.CUSTOMER_DETAIL)
    public ApiResult<CustomerSite> get(@PathVariable Long id) {
        return ApiResult.ok(customerSiteService.get(id));
    }

    @PostMapping
    @RequirePermission(PermissionCode.CUSTOMER_CREATE)
    public ApiResult<CustomerSite> create(@RequestBody CustomerSiteRequest request) {
        return ApiResult.ok(customerSiteService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission(PermissionCode.CUSTOMER_EDIT)
    public ApiResult<CustomerSite> update(@PathVariable Long id, @RequestBody CustomerSiteRequest request) {
        return ApiResult.ok(customerSiteService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequirePermission(PermissionCode.CUSTOMER_STATUS)
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        customerSiteService.updateStatus(id, status);
        return ApiResult.ok();
    }
}
