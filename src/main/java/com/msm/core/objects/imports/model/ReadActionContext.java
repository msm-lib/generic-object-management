package com.msm.core.objects.imports.model;



import java.util.function.Consumer;

public record ReadActionContext<T>(
        String importObjectName,
        String fileUrl,
        Consumer<RawRow<T>> rowConsumer
) {
    public static <T> ReadActionContext<T> of(String importObjectName, String fileUrl, Consumer<RawRow<T>> rowConsumer) {
        return new ReadActionContext<>(importObjectName, fileUrl, rowConsumer);
    }
}
