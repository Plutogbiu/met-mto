package com.met.mto.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.met.mto.dto.MapPlaceSuggestion;
import com.met.mto.exception.BusinessException;
import com.met.mto.exception.ErrorCode;
import com.met.mto.service.TencentMapService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TencentMapServiceImpl implements TencentMapService {

    private static final String SUGGESTION_URL = "https://apis.map.qq.com/ws/place/v1/suggestion";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mto.map.tencent.key}")
    private String tencentMapKey;

    @Override
    public List<MapPlaceSuggestion> suggestPlaces(String keyword, String region) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        if (!StringUtils.hasText(tencentMapKey)) {
            throw new BusinessException(ErrorCode.MAP_KEY_NOT_CONFIGURED);
        }

        String uri = UriComponentsBuilder.fromUriString(SUGGESTION_URL)
                .queryParam("keyword", keyword.trim())
                .queryParam("region", StringUtils.hasText(region) ? region.trim() : "全国")
                .queryParam("region_fix", 0)
                .queryParam("page_size", 10)
                .queryParam("key", tencentMapKey)
                .build()
                .encode()
                .toUriString();

        JsonNode response = restTemplate.getForObject(uri, JsonNode.class);

        if (response == null) {
            return Collections.emptyList();
        }
        int status = response.path("status").asInt(-1);
        if (status != 0) {
            String message = response.path("message").asText("腾讯地图地址解析失败");
            throw new BusinessException(ErrorCode.MAP_REQUEST_FAILED, message);
        }

        List<MapPlaceSuggestion> suggestions = new ArrayList<>();
        for (JsonNode item : response.path("data")) {
            MapPlaceSuggestion suggestion = new MapPlaceSuggestion();
            suggestion.setTitle(item.path("title").asText(""));
            suggestion.setAddress(item.path("address").asText(""));
            suggestion.setProvince(item.path("ad_info").path("province").asText(""));
            suggestion.setCity(item.path("ad_info").path("city").asText(""));
            suggestion.setDistrict(item.path("ad_info").path("district").asText(""));
            suggestion.setLongitude(readDecimal(item.path("location").path("lng")));
            suggestion.setLatitude(readDecimal(item.path("location").path("lat")));
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    private BigDecimal readDecimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return BigDecimal.valueOf(node.asDouble());
    }
}
