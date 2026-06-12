package com.msm.core.objects.service.imports;

import java.util.List;

public interface FileReader<F, T> {
    List<T> read(String objectNme, F source);
}
