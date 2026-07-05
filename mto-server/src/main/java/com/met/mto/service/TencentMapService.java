package com.met.mto.service;

import com.met.mto.dto.MapPlaceSuggestion;
import java.util.List;

public interface TencentMapService {

    List<MapPlaceSuggestion> suggestPlaces(String keyword, String region);
}
