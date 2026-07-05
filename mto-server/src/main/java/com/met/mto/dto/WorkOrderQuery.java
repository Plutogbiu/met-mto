package com.met.mto.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderQuery {

    private String keyword;

    private String type;

    private String status;

    private String customerKeyword;

    private Boolean unfinishedOnly;

    private Long customerSiteId;

    private Long engineerId;

    private LocalDateTime createdStart;

    private LocalDateTime createdEnd;

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
