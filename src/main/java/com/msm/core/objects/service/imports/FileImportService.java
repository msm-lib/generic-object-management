package com.msm.core.objects.service.imports;

import com.msm.core.action.annotations.action.Handler;
import com.msm.core.action.context.ActionContext;
import com.msm.core.commons.Constants;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class FileImportService {

    private static final long MAX_CODE_IMPORT = 50000;
    private final MultipartCsvObjectReader multipartCsvObjectReader;

    @Handler(action = Constants.Action.IMPORT_FILE_OBJECT)
    public void importFile(ActionContext<MultipartFile> actionRequest) {
        MultipartFile payload = actionRequest.getPayload();
        try {
            List<Map<String, Object>> items = multipartCsvObjectReader.read(actionRequest.getResource(), payload);
            log.debug("Voucher file imported successfully {}", items);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw Lombok.sneakyThrow(e);
        }
    }
}
