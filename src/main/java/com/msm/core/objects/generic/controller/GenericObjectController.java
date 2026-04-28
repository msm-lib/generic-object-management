package com.msm.core.objects.generic.controller;

import com.msm.core.filter.domain.ObjectFilterRequest;
import com.msm.core.filter.domain.PageResponse;
import com.msm.core.metadata.ObjectMetadata;
import com.msm.core.objects.generic.dto.ObjectConversionRequest;
import com.msm.core.objects.generic.dto.QueryTemplate;
import com.msm.core.objects.generic.service.GenericObjectMetadataService;
import com.msm.core.objects.generic.service.GenericObjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <pre>Construct api path:</pre>
 * <pre>/serviceName</pre>
 *   <pre> -> /api/v1</pre>
 *       <pre>      -> /cn</pre>
 *          <pre>           -> /mobile</pre>
 *          <pre>           -> /portal</pre>
 *       <pre>      -> /external</pre>
 *       <pre>      -> /internal</pre>
 */
@Tag(name = "Generic objects controller")
@RestController
@RequestMapping("/api/v1/cn/portal")
@RequiredArgsConstructor
public class GenericObjectController {

    private final GenericObjectService genericObjectService;
    private final GenericObjectMetadataService genericObjectMetadataService;


    @Operation(summary = "Filter object", description = "list of object")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = PageResponse.class)))})
    @PostMapping("/generic/objects/{objectName}/filter")
    public ResponseEntity<PageResponse<Object>> filterObjects(
            @PathVariable("objectName") String objectName,
            @RequestBody ObjectFilterRequest filter) {

        filter.setObjectInfo(ObjectFilterRequest.ObjectInfo.builder().name(objectName).build());
        return ResponseEntity.ok(genericObjectService.filter(filter));
    }

    @Operation(summary = "Get object by id", description = "Returns object")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = Object.class)))})
    @GetMapping("/generic/objects/{objectName}/{id}")
    public ResponseEntity<Object> getObjectById(
            @PathVariable("objectName") String objectName,
            @PathVariable("id") UUID id,
            @RequestParam(value = "returnFields", required = false) List<String> returnFields) {
        return ResponseEntity.ok(genericObjectService.getObjectById(objectName, id, returnFields));
    }

    @Operation(summary = "Get all objects", description = "Returns list of object")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = List.class)))})
    @GetMapping("/generic/objects/{objectName}")
    public ResponseEntity<List<Object>> getAllObjects(
            @PathVariable("objectName") String objectName,
            @RequestParam(value = "returnFields", required = false) List<String> returnFields) {

        return ResponseEntity.ok(genericObjectService.getAllObject(objectName, returnFields));
    }

    @Operation(summary = "Create generic object", description = "Returns object created")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = Object.class)))})
    @PostMapping("/generic/objects/{objectName}")
    public ResponseEntity<Object> createObjects(
            @PathVariable("objectName") String objectName,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(genericObjectService.createObject(objectName, request));
    }

    @Operation(summary = "Create multiple generic object", description = "Returns objects created")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = Object.class)))})
    @PostMapping("/generic/objects/{objectName}/bulk")
    public ResponseEntity<Object> createBulkObject(
            @PathVariable("objectName") String objectName,
            @RequestBody List<Map<String, Object>> request) {
        return ResponseEntity.ok(genericObjectService.createObjects(objectName, request));
    }

    @Operation(summary = "Update generic object", description = "Returns object updated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = Object.class)))})
    @PutMapping("/generic/objects/{objectName}/{id}")
    public ResponseEntity<Object> updateObject(
            @PathVariable("objectName") String objectName,
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(genericObjectService.updateObject(objectName, id, request));
    }

    @Operation(summary = "Update generic object", description = "Returns id deleted")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = Object.class)))})
    @DeleteMapping("/generic/objects/{objectName}/{id}")
    public ResponseEntity<Object> deleteObject(
            @PathVariable("objectName") String objectName,
            @PathVariable("id") UUID id) {
        genericObjectService.deleteObject(objectName, id);
        return ResponseEntity.ok(id);
    }

    @Operation(summary = "Delete multiple generic object", description = "Returns ids deleted")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = Object.class)))})
    @DeleteMapping("/generic/objects/{objectName}/bulk")
    public ResponseEntity<Object> deleteObjects(
            @PathVariable("objectName") String objectName,
            @RequestBody List<UUID> ids) {
        genericObjectService.deleteObject(objectName, ids);
        return ResponseEntity.ok(ids);
    }

    @Operation(summary = "Conversion object", description = "Returns ids deleted")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = Object.class)))})
    @PostMapping("/generic/objects/conversion")
    public ResponseEntity<Object> conversionObject(@RequestBody ObjectConversionRequest request) {
        return ResponseEntity.ok(genericObjectService.conversion(request));
    }

    @Operation(summary = "Create generic query base on request", description = "Returns any object base on query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = Object.class)))})
    @PostMapping("/generic/objects/query")
    public ResponseEntity<Object> createQuery(
            @RequestBody QueryTemplate request) {
        return ResponseEntity.ok(genericObjectService.queryTemplate(request));
    }

    @Operation(
            summary = "Create generic meta data",
            description = """
    ### Request Payload

    ```json
    {
      "tableName": "table_name",
      "schema": "bhc",
      "serviceName": "order",
      "excludeRef": ["id"],
      "freeTextColumns": ["description", "note"],
      "requiredColumns": ["name", "code"]
    }
    ```

    ### Notes
    - `excludeRef`: list column not generate ref
    - `freeTextColumns`: columns are free text
    - `requiredColumns`: columns are required if null that will check nullable of db column
    """
    )


//    @Operation(summary = "Create generic meta data", description = "Returns generic object meta data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = Object.class)))})
    @PostMapping("/generic/objects/metadata")
    public ResponseEntity<ObjectMetadata> createObjectMetadata(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(genericObjectMetadataService.autoGenerateObjectMetaDataHandler(request));
    }
}
