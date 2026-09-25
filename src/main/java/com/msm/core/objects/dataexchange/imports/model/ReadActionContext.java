package com.msm.core.objects.dataexchange.imports.model;



import java.util.UUID;
import java.util.function.Consumer;

public record ReadActionContext<T>(
        String importObjectName,
        UUID importId,
        String fileUrl,
        Consumer<RawRow<T>> rowConsumer
) {
    public static <T> ReadActionContext<T> of(String importObjectName, UUID importId, String fileUrl, Consumer<RawRow<T>> rowConsumer) {
        return new ReadActionContext<>(importObjectName, importId, fileUrl, rowConsumer);
    }
}
