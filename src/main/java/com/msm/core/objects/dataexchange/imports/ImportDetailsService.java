package com.msm.core.objects.dataexchange.imports;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.action.executor.ActionExecutor;
import com.msm.core.commons.Constants;
import com.msm.core.commons.Utils;
import com.msm.core.dynamicquery.ObjectMetadataFactory;
import com.msm.core.exceptions.CommonErrors;
import com.msm.core.filter.domain.FilterCondition;
import com.msm.core.filter.domain.FilterGroup;
import com.msm.core.filter.domain.FilterOperator;
import com.msm.core.filter.domain.LogicalOperator;
import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.metadata.Attribute;
import com.msm.core.metadata.typesafe.DataRecord;
import com.msm.core.objects.ObjectActionNamed;
import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.dataexchange.ErrorHelper;
import com.msm.core.objects.dataexchange.imports.metadata.S3FileInfoMeta;
import com.msm.core.objects.dataexchange.imports.model.ImportContext;
import com.msm.core.objects.dataexchange.imports.model.ImportMode;
import com.msm.core.objects.dataexchange.imports.model.ImportParentContext;
import com.msm.core.objects.dataexchange.imports.model.ImportParentResult;
import com.msm.core.objects.dataexchange.imports.model.ImportStatus;
import com.msm.core.objects.dataexchange.imports.model.ObjectImportContext;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.exception.ObjectErrors;
import com.msm.core.objects.repository.ObjectQueryRepository;
import com.msm.core.security.annotations.SecuredPermissionAction;
import com.msm.core.security.enums.PermissionAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Import of the details of one header: {@code PUT /generic/objects/{header}/{id}/import_details} with
 * {@code {importJobId, version, mode}}. The job ({@code objectName} = a detail object whose imports config has
 * {@code parent.object} = the header) was created and its file uploaded as for any import.
 * <ol>
 *   <li>reads the header with the user's data scope (404), checks its version (409);</li>
 *   <li>checks the job (UPLOADING, its object's parent is this header, not bound to another header) and the mode;</li>
 *   <li>dispatches {@code import_parent_check} so the service may refuse (e.g. the header can no longer change);</li>
 *   <li>stores the header in {@code fileInfo.context}, then validates and imports the file: every row gets the header
 *       id and snapshot, the identity is within the header, APPEND refuses rows the header already has;</li>
 *   <li>dispatches {@code import_parent_completed} once when rows were written, e.g. to recompute the header.</li>
 * </ol>
 * The route checks the permission of the action on the header ({@code canProcessAction(header, import_details)}): the
 * user needs UPDATE on the header object.
 * Response: {@code {job, parent}} (the header read again).
 */
@Slf4j
@RequiredArgsConstructor
public class ImportDetailsService {
    public static final String JOB = "job";
    public static final String PARENT = "parent";
    private static final String IMPORT_JOB_ID = "importJobId";
    private static final String VERSION = "version";
    private static final String MODE = "mode";
    private static final String CODE = "code";
    private static final String CSV = "csv";

    private final ActionExecutor actionExecutor;
    private final ObjectQueryRepository internalObjectQueryRepository;
    private final ImportConfigService importConfigService;
    private final ImportJobService importJobService;

    @Handler(action = ObjectActionNamed.ImportDetails.IMPORT_DETAILS)
    @SecuredPermissionAction(anyOf = PermissionAction.UPDATE)
    public Map<String, Object> importDetails(ActionContext<Map<String, Object>> actionContext) {
        String headerObject = actionContext.getResource();
        UUID headerId = actionContext.getObjectIdAs(UUID.class);
        DataRecord request = DataRecord.ofNullable(actionContext.getPayload());

        Map<String, Object> header = readHeader(headerObject, headerId);
        checkVersion(headerObject, header, request.get(VERSION, Long.class));

        UUID jobId = request.getOptional(IMPORT_JOB_ID, UUID.class)
                .orElseThrow(() -> CommonErrors.required(IMPORT_JOB_ID, IMPORT_JOB_ID + " is required"));
        DataRecord job = DataRecord.ofNullable(internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobId));
        if (job.getValues().isEmpty()) {
            throw ObjectErrors.notFound(ImportJobMeta.OBJECT_NAME);
        }
        String detailObject = job.get(ImportJobMeta.OBJECT_NAME_FIELD);
        ObjectImportRegistry.ParentConfig parent = importConfigService.getParent(detailObject);
        checkJob(headerObject, headerId, jobId, job, detailObject, parent);
        ImportMode mode = mode(request.get(MODE, String.class), parent);

        DataRecord headerRecord = DataRecord.of(header);
        String versionField = versionField(headerObject);
        ImportContext context = new ImportContext(headerObject, headerId, headerRecord.get(CODE, String.class),
                versionField == null ? null : headerRecord.get(versionField, Long.class), mode, snapshot(header, parent.getReferenceFields()));
        actionExecutor.execute(ActionContext.<ImportParentContext>builder()
                .resource(detailObject)
                .action(ObjectActionNamed.ImportDetails.PARENT_CHECK)
                .payload(ImportParentContext.of(jobId, detailObject, context, header))
                .build());
        importJobService.saveContext(jobId, context);

        DataRecord imported = DataRecord.ofNullable(actionExecutor.execute(ActionContext.<ObjectImportContext>builder()
                .resource(detailObject)
                .action(ObjectActionNamed.Excel.IMPORT_FILE)
                .payload(ObjectImportContext.of(detailObject, jobId, null))
                .build()));
        completeParent(jobId, detailObject, context, header, imported);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put(JOB, internalObjectQueryRepository.findById(ImportJobMeta.OBJECT_NAME, jobId));
        response.put(PARENT, readHeader(headerObject, headerId));
        return response;
    }

    /** No check unless the service has an {@code @ImportParentCheckHandler} for the detail object. */
    @Handler(action = ObjectActionNamed.ImportDetails.PARENT_CHECK)
    public Object parentCheck(ActionContext<ImportParentContext> actionContext) {
        return null;
    }

    /** Nothing to do unless the service has an {@code @ImportParentCompletedHandler} for the detail object. */
    @Handler(action = ObjectActionNamed.ImportDetails.PARENT_COMPLETED)
    public Object parentCompleted(ActionContext<ImportParentResult> actionContext) {
        return null;
    }

    /** The header as the user may see it (data scope), like GET /{object}/{id}. */
    private Map<String, Object> readHeader(String headerObject, UUID headerId) {
        FilterGroup filters = new FilterGroup();
        filters.setOperator(LogicalOperator.AND);
        filters.setConditions(Utils.CL.newArrayList(FilterCondition.create(Constants.OBJECT_PK, FilterOperator.EQUALS, headerId)));
        ActionContext<ObjectFilterRequest> read = ActionContext.<ObjectFilterRequest>builder()
                .resource(headerObject)
                .action(Constants.FilterAction.FILTER_OBJECT_BY_ID)
                .payload(ObjectFilterRequest.builder().objectInfo(ObjectFilterRequest.ObjectInfo.of(headerObject)).filters(filters).build())
                .build();
        read.setObjectId(headerId);
        Map<String, Object> header = actionExecutor.execute(read);
        if (Utils.CL.isEmpty(header)) {
            throw ObjectErrors.notFound(headerObject);
        }
        return header;
    }

    private void checkVersion(String headerObject, Map<String, Object> header, Long requestVersion) {
        String versionField = versionField(headerObject);
        if (versionField == null) {
            return;
        }
        if (requestVersion == null) {
            throw CommonErrors.required(VERSION, "The version of the " + headerObject + " is required");
        }
        if (!Objects.equals(requestVersion, DataRecord.of(header).get(versionField, Long.class))) {
            throw CommonErrors.optimisticLockingFailureException(headerObject,
                    "The " + headerObject + " was changed by someone else; reload it and import again");
        }
    }

    private void checkJob(String headerObject, UUID headerId, UUID jobId, DataRecord job, String detailObject,
                          ObjectImportRegistry.ParentConfig parent) {
        if (parent == null || !headerObject.equals(parent.getObject())) {
            throw ObjectErrors.payloadInvalidException(ImportJobMeta.OBJECT_NAME,
                    "Import job " + jobId + " (" + detailObject + ") does not import details of " + headerObject, null);
        }
        if (!ImportStatus.UPLOADING.name().equals(job.get(ImportJobMeta.STATUS))) {
            throw ObjectErrors.payloadInvalidException(ImportJobMeta.OBJECT_NAME,
                    "Import job " + jobId + " was already processed (status " + job.get(ImportJobMeta.STATUS) + ")", null);
        }
        Map<String, Object> fileInfo = Utils.CL.emptyIfNull(job.get(ImportJobMeta.FILE_INFO));
        ImportContext bound = ImportContext.from(fileInfo.get(ImportJobService.CONTEXT));
        if (bound != null && !headerId.equals(bound.parentId())) {
            throw ObjectErrors.payloadInvalidException(ImportJobMeta.OBJECT_NAME,
                    "Import job " + jobId + " imports into another " + headerObject, null);
        }
        if (CSV.equalsIgnoreCase(String.valueOf(fileInfo.get(S3FileInfoMeta.FILE_TYPE.getFieldName())))) {
            throw ObjectErrors.payloadInvalidException(ImportJobMeta.OBJECT_NAME,
                    "Import into a " + headerObject + " takes an Excel file", null);
        }
    }

    private ImportMode mode(String requestMode, ObjectImportRegistry.ParentConfig parent) {
        if (Utils.STR.isBlank(requestMode)) {
            return parent.getDefaultMode();
        }
        String name = requestMode.trim();
        ImportMode mode = Arrays.stream(ImportMode.values())
                .filter(value -> value.name().equals(name))
                .findFirst()
                .orElseThrow(() -> CommonErrors.invalid(MODE, "Unknown import mode " + name));
        if (!parent.getModes().contains(mode)) {
            throw CommonErrors.invalid(MODE, "Import mode " + mode + " is not allowed here; allowed: " + parent.getModes());
        }
        return mode;
    }

    /** Once rows were written: the service's completion (header recomputed…); a failure is reported in the job. */
    private void completeParent(UUID jobId, String detailObject, ImportContext context, Map<String, Object> header, DataRecord imported) {
        String status = imported.get(ImportJobMeta.STATUS);
        long successRows = imported.getOrDefault(ImportJobMeta.SUCCESS_ROWS, 0L);
        boolean written = ImportStatus.COMPLETED.name().equals(status) || ImportStatus.COMPLETED_WITH_ERRORS.name().equals(status);
        if (!written || successRows == 0) {
            return;
        }
        try {
            actionExecutor.execute(ActionContext.<ImportParentResult>builder()
                    .resource(detailObject)
                    .action(ObjectActionNamed.ImportDetails.PARENT_COMPLETED)
                    .payload(ImportParentResult.of(jobId, detailObject, context, header, successRows,
                            imported.getOrDefault(ImportJobMeta.ERROR_ROWS, 0L)))
                    .build());
        } catch (Exception e) {
            log.error("Completing the {} {} after import job {} failed", context.parentObject(), context.parentId(), jobId, e);
            internalObjectQueryRepository.update(ImportJobMeta.OBJECT_NAME, jobId, DataRecord.of()
                    .with(ImportJobMeta.STATUS, ImportStatus.COMPLETED_WITH_ERRORS.name())
                    .with(ImportJobMeta.ERROR_MESSAGE, "Rows imported, but updating the " + context.parentObject() + " failed: "
                            + ErrorHelper.resolveErrorMessage(e))
                    .getValues());
        }
    }

    private static String versionField(String headerObject) {
        Attribute versionAttribute = ObjectMetadataFactory.getObjectMetadataByName(headerObject).getVersionAttribute();
        return versionAttribute == null ? null : versionAttribute.getFieldName();
    }

    private static Map<String, Object> snapshot(Map<String, Object> header, List<String> fields) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        Utils.CL.emptyIfNull(fields).forEach(field -> snapshot.put(field, header.get(field)));
        return snapshot;
    }
}
