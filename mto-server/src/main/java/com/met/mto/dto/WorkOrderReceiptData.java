package com.met.mto.dto;

import java.util.List;
import lombok.Data;

@Data
public class WorkOrderReceiptData {

    private String companyLogoDataUri;

    private String orderNo;
    private String type;
    private String createdAt;
    private String completedAt;
    private String customerName;
    private String customerAddress;
    private String deviceName;
    private String maintenanceContent;
    private String content;
    private String creatorName;
    private List<String> engineers;
    private List<WorkOrderReceiptImage> images;
    private String imageSectionTitle;
    private String generatedAt;
}
