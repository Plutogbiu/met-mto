package com.met.mto.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MapPlaceSuggestion {

    private String title;

    private String address;

    private String province;

    private String city;

    private String district;

    private BigDecimal longitude;

    private BigDecimal latitude;
}
