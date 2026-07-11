package com.met.mto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.met.mto.dto.AppVersionResponse;
import com.met.mto.entity.AppVersion;
import com.met.mto.mapper.AppVersionMapper;
import com.met.mto.service.AppVersionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AppVersionServiceImpl implements AppVersionService {

    private static final String DEFAULT_PLATFORM = "android";

    private final AppVersionMapper appVersionMapper;

    @Override
    public AppVersionResponse latest(String platform, Integer currentVersionCode) {
        String normalizedPlatform = StringUtils.hasText(platform) ? platform.trim().toLowerCase() : DEFAULT_PLATFORM;
        int currentCode = currentVersionCode == null ? 0 : currentVersionCode;
        List<AppVersion> versions = appVersionMapper.selectList(new LambdaQueryWrapper<AppVersion>()
                .eq(AppVersion::getPlatform, normalizedPlatform)
                .eq(AppVersion::getStatus, 1)
                .orderByDesc(AppVersion::getVersionCode)
                .orderByDesc(AppVersion::getId));
        AppVersion version = versions.stream()
                .filter(item -> isCompatible(item, currentCode))
                .findFirst()
                .orElse(null);
        if (version == null) {
            AppVersionResponse response = new AppVersionResponse();
            response.setNeedUpdate(false);
            response.setPlatform(normalizedPlatform);
            response.setForceUpdate(false);
            return response;
        }
        return toResponse(version, currentCode);
    }

    private boolean isCompatible(AppVersion version, int currentVersionCode) {
        if (!"wgt".equalsIgnoreCase(version.getUpdateType())) {
            return true;
        }
        Integer baseVersionCode = version.getBaseVersionCode();
        return baseVersionCode == null || baseVersionCode <= 0 || currentVersionCode >= baseVersionCode;
    }

    private AppVersionResponse toResponse(AppVersion version, int currentVersionCode) {
        AppVersionResponse response = new AppVersionResponse();
        response.setPlatform(version.getPlatform());
        response.setUpdateType(version.getUpdateType());
        response.setVersionName(version.getVersionName());
        response.setVersionCode(version.getVersionCode());
        response.setMinVersionCode(version.getMinVersionCode());
        response.setBaseVersionCode(version.getBaseVersionCode());
        response.setDownloadUrl(version.getDownloadUrl());
        response.setReleaseNotes(version.getReleaseNotes());

        int latestVersionCode = version.getVersionCode() == null ? 0 : version.getVersionCode();
        int minVersionCode = version.getMinVersionCode() == null ? 0 : version.getMinVersionCode();
        boolean needUpdate = currentVersionCode < latestVersionCode;
        boolean forceUpdate = (version.getForceUpdate() != null && version.getForceUpdate() == 1)
                || (minVersionCode > 0 && currentVersionCode < minVersionCode);
        response.setNeedUpdate(needUpdate);
        response.setForceUpdate(forceUpdate);
        return response;
    }
}
