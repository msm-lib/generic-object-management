package com.msm.core.objects.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.msm.core.action.annotations.action.CreateHandler;
import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Constants;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.entity.metadata.ExportJobMeta;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.imports.dtometda.AttachmentInfoMeta;
import com.msm.core.objects.imports.dtometda.S3FileInfoMeta;
import com.msm.core.objects.imports.excel.ExportExcelService;
import com.msm.core.objects.imports.model.ExportStatus;
import com.msm.core.objects.imports.s3.S3FileUtils;
import com.msm.core.objects.repository.ObjectQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ExportJobService {
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ExportExcelService exportExcelService;
    private final S3FileUtils s3FileUtils;

    @Handler(resource = ExportJobMeta.OBJECT_NAME, action = Constants.FilterAction.FILTER_OBJECT_BY_ID)
    public Map<String, Object> filterExportJob(ActionContext<ObjectFilterRequest> actionContext) {
        UUID objectId = actionContext.getObjectIdAs(UUID.class);
        Map<String, Object> objectMap = internalObjectQueryRepository.findById(ExportJobMeta.OBJECT_NAME,  objectId);
        DataRecord exportJobRecord = DataRecord.of(objectMap);
        DataRecord s3FileInfoRecord = DataRecord.ofNullable(exportJobRecord.get(ExportJobMeta.FILE_INFO));
        String s3Key = s3FileInfoRecord.get(S3FileInfoMeta.S3_KEY);
        if(Objects.nonNull(s3Key)) {
            DataRecord attachmentRecord = DataRecord.of(s3FileUtils.getDownloadInfo(exportJobRecord.get(ExportJobMeta.OBJECT_NAME_FIELD), s3FileInfoRecord));
            String fileUrl = attachmentRecord.get(AttachmentInfoMeta.DOWNLOAD_URL);
            exportJobRecord.with(AttachmentInfoMeta.DOWNLOAD_URL, fileUrl);
            exportJobRecord.remove(ExportJobMeta.FILE_INFO);
        }

        return exportJobRecord.asMap();
    }

//    @HookAfter(resource = ExportJobMeta.OBJECT_NAME, action = Constants.FilterAction.FILTER_OBJECT)
//    public Map<String, Object> afterFilterExportJob(ActionContext<ObjectFilterRequest> actionContext) {
//        UUID objectId = actionContext.getObjectIdAs(UUID.class);
//        Map<String, Object> objectMap = internalObjectQueryRepository.findById(ExportJobMeta.OBJECT_NAME,  objectId);
//        DataRecord exportJobRecord = DataRecord.of(objectMap);
//        DataRecord s3FileInfoRecord = DataRecord.of(exportJobRecord.get(ExportJobMeta.FILE_INFO));
//
//        String s3Key = s3FileInfoRecord.get(S3FileInfoMeta.S3_KEY);
//        if(Objects.nonNull(s3Key)) {
//            DataRecord attachmentRecord = DataRecord.of(s3FileUtils.getDownloadInfo(exportJobRecord.get(ExportJobMeta.OBJECT_NAME_FIELD), s3FileInfoRecord));
//            String fileUrl = attachmentRecord.get(AttachmentInfoMeta.DOWNLOAD_URL);
//            exportJobRecord.with(AttachmentInfoMeta.DOWNLOAD_URL, fileUrl);
//            exportJobRecord.remove(ExportJobMeta.FILE_INFO);
//        }
//
//        return exportJobRecord.asMap();
//    }

    @CreateHandler(resource = ExportJobMeta.OBJECT_NAME)
    public Map<String, Object> exportJob(ActionContext<Map<String, Object>> actionContext) {
        //{
        //    "objectName": "profile",
        //    "service": "customer",
        //    "fileName": "Import_profile_25082026.xlsx",
        //    "originalFileName": "Import_profile_25082026.xlsx",
        //    "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        //    "contentLength": 240675,
        //    "description": ""
        //}
        // bhc/order/exportjob/20260919/file.xlsx

        UUID exportId = UUID.randomUUID();
        Map<String, Object> payload = actionContext.getPayload();
        DataRecord payloadRecord = DataRecord.of(payload);
        DataRecord exportJobRecord = DataRecord.of()
                .with(ExportJobMeta.ID, exportId)
                .with(ExportJobMeta.OBJECT_NAME_FIELD, payloadRecord.get(ExportJobMeta.OBJECT_NAME_FIELD))
                .with(ExportJobMeta.FILTER_CRITERIA, payloadRecord.get(ExportJobMeta.FILTER_CRITERIA.getFieldName(), new TypeReference<>() {}))
                .with(ExportJobMeta.STATUS, ExportStatus.PENDING_EXPORT.name())
                .with(ImportJobMeta.STARTED_AT, Instant.now());

        Map<String, Object> exportJobMap = internalObjectQueryRepository.save(ExportJobMeta.OBJECT_NAME, exportJobRecord.getValues());

        exportExcelService.exportExcelData(s3FileUtils.getOrDefaultRootFolder(), exportJobMap);
        log.info("Export job has been successfully completed");
        return exportJobMap;
    }
}
