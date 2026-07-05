package com.met.mto.service;

import com.met.mto.common.PageResult;
import com.met.mto.dto.CustomerSiteQuery;
import com.met.mto.dto.CustomerSiteRequest;
import com.met.mto.entity.CustomerSite;

public interface CustomerSiteService {

    PageResult<CustomerSite> page(CustomerSiteQuery query);

    CustomerSite get(Long id);

    CustomerSite create(CustomerSiteRequest request);

    CustomerSite update(Long id, CustomerSiteRequest request);

    void updateStatus(Long id, Integer status);
}
