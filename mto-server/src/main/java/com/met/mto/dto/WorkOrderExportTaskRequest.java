package com.met.mto.dto;

import java.util.List;
import lombok.Data;

@Data
public class WorkOrderExportTaskRequest {

    private List<Long> workOrderIds;

    private Boolean allFiltered;

    private String keyword;

    private String type;

    private String createdStart;

    private String createdEnd;

    private String completedStart;

    private String completedEnd;
}
