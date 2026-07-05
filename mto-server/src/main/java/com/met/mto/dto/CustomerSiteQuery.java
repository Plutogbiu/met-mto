package com.met.mto.dto;

import lombok.Data;

@Data
public class CustomerSiteQuery {

    private String keyword;

    private Integer status;

    private long page = 1;

    private long size = 10;

    public long safePage() {
        return page <= 0 ? 1 : page;
    }

    public long safeSize() {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
