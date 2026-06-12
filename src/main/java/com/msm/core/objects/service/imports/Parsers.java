package com.msm.core.objects.service.imports;

import com.msm.core.commons.Utils;

import java.util.Arrays;
import java.util.List;

public class Parsers {
    public static List<String> arrayParser(String resource) {
        if (Utils.STR.isBlank(resource)) {
            return Utils.CL.newArrayList();
        }
        return  Arrays.asList(resource.replaceAll("[{}]", "").split(","));
    }
}
