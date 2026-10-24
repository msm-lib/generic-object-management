package com.msm.core.objects.dataexchange.imports;

import com.msm.core.objects.dataexchange.imports.s3.S3FileUtils;
import com.msm.core.objects.entity.metadata.ImportJobMeta;
import com.msm.core.objects.repository.ObjectQueryRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ImportJobServiceTest {

    private final ObjectQueryRepository repository = mock(ObjectQueryRepository.class);
    private final ImportJobService service = new ImportJobService(repository, mock(S3FileUtils.class));

    @Test
    void sheetRunBelongsToItsJobUntilItEnds() {
        UUID jobId = UUID.randomUUID();

        UUID runId = service.startSheetRun(jobId);

        assertThat(runId).isNotEqualTo(jobId);
        assertThat(service.jobIdOf(runId)).isEqualTo(jobId);
        service.endSheetRun(runId);
        assertThat(service.jobIdOf(runId)).isEqualTo(runId);
    }

    @Test
    void importIdThatIsNotARunIsItsOwnJob() {
        UUID jobId = UUID.randomUUID();

        assertThat(service.jobIdOf(jobId)).isEqualTo(jobId);
        assertThat(service.jobIdOf(null)).isNull();
    }

    @Test
    void statusOfASheetRunIsWrittenToItsJob() {
        UUID jobId = UUID.randomUUID();
        UUID runId = service.startSheetRun(jobId);

        service.makeJobValidating(runId);
        service.makeJobImporting(runId);
        service.makeJobValidateFailed(runId, "bad file");
        service.makeJobFailed(runId, "boom");

        verify(repository, org.mockito.Mockito.times(4)).update(eq(ImportJobMeta.OBJECT_NAME), eq(jobId), anyMap());
    }
}
