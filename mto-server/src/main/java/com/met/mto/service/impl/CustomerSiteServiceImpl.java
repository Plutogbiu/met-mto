package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.met.mto.common.PageResult;
import com.met.mto.dto.CustomerSiteQuery;
import com.met.mto.dto.CustomerSiteRequest;
import com.met.mto.entity.CustomerSite;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.mapper.CustomerSiteMapper;
import com.met.mto.service.CustomerSiteService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerSiteServiceImpl implements CustomerSiteService {

    private final CustomerSiteMapper customerSiteMapper;

    @Override
    public PageResult<CustomerSite> page(CustomerSiteQuery query) {
        LambdaQueryWrapper<CustomerSite> wrapper = new LambdaQueryWrapper<CustomerSite>()
                .like(StringUtils.hasText(query.getKeyword()), CustomerSite::getName, query.getKeyword())
                .eq(query.getStatus() != null, CustomerSite::getStatus, query.getStatus())
                .orderByDesc(CustomerSite::getUpdatedAt)
                .orderByDesc(CustomerSite::getId);

        Page<CustomerSite> page = customerSiteMapper.selectPage(new Page<>(query.safePage(), query.safeSize()), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public CustomerSite get(Long id) {
        return customerSiteMapper.selectById(id);
    }

    @Override
    @Transactional
    public CustomerSite create(CustomerSiteRequest request) {
        checkName(request.getName());
        CustomerSite site = new CustomerSite();
        apply(site, request);
        site.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        site.setCreatedAt(LocalDateTime.now());
        site.setUpdatedAt(site.getCreatedAt());
        customerSiteMapper.insert(site);
        return site;
    }

    @Override
    @Transactional
    public CustomerSite update(Long id, CustomerSiteRequest request) {
        checkName(request.getName());
        CustomerSite site = customerSiteMapper.selectById(id);
        if (site == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_SITE_NOT_FOUND);
        }
        apply(site, request);
        if (request.getStatus() != null) {
            site.setStatus(request.getStatus());
        }
        site.setUpdatedAt(LocalDateTime.now());
        customerSiteMapper.updateById(site);
        return site;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        CustomerSite site = customerSiteMapper.selectById(id);
        if (site == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_SITE_NOT_FOUND);
        }
        site.setStatus(status == null ? 1 : status);
        site.setUpdatedAt(LocalDateTime.now());
        customerSiteMapper.updateById(site);
    }

    private void apply(CustomerSite site, CustomerSiteRequest request) {
        site.setName(request.getName());
        site.setAddress(request.getAddress());
        site.setContactName(request.getContactName());
        site.setContactPhone(request.getContactPhone());
        site.setLongitude(request.getLongitude());
        site.setLatitude(request.getLatitude());
        site.setLocationAddress(request.getLocationAddress());
        site.setLocationRemark(request.getLocationRemark());
        site.setRemark(request.getRemark());
    }

    private void checkName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.CUSTOMER_SITE_NAME_REQUIRED);
        }
    }
}
