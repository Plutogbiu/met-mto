package com.met.mto.service;

import com.met.mto.dto.WorkOrderReceiptFile;
import java.io.OutputStream;

public interface WorkOrderReceiptPdfService {

    WorkOrderReceiptFile export(Long workOrderId);

    String exportTo(Long workOrderId, OutputStream output);
}
