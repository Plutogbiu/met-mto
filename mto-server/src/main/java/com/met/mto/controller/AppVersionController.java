package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.dto.AppVersionResponse;
import com.met.mto.service.AppVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/version")
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionService appVersionService;

    @GetMapping("/latest")
    public ApiResult<AppVersionResponse> latest(
            @RequestParam(required = false, defaultValue = "android") String platform,
            @RequestParam(required = false) Integer currentVersionCode
    ) {
        return ApiResult.ok(appVersionService.latest(platform, currentVersionCode));
    }
}
