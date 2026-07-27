package com.met.mto.dto;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkOrderExportDownloadFile {

    private String fileName;
    private Path path;
    private long fileSize;
}
