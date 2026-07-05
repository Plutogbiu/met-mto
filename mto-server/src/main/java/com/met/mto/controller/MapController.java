package com.met.mto.controller;

import com.met.mto.common.ApiResult;
import com.met.mto.dto.MapConfigResponse;
import com.met.mto.dto.MapPlaceSuggestion;
import com.met.mto.service.TencentMapService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/maps")
@RequiredArgsConstructor
public class MapController {

    private final TencentMapService tencentMapService;

    @Value("${mto.map.tencent.key}")
    private String tencentMapKey;

    @Value("${mto.map.tencent.referer:MET-MTO}")
    private String tencentMapReferer;

    @GetMapping("/config")
    public ApiResult<MapConfigResponse> config() {
        MapConfigResponse response = new MapConfigResponse();
        response.setKey(tencentMapKey);
        response.setReferer(tencentMapReferer);
        return ApiResult.ok(response);
    }

    @GetMapping("/place-suggestions")
    public ApiResult<List<MapPlaceSuggestion>> suggestPlaces(
            @RequestParam String keyword,
            @RequestParam(required = false) String region
    ) {
        return ApiResult.ok(tencentMapService.suggestPlaces(keyword, region));
    }
}
