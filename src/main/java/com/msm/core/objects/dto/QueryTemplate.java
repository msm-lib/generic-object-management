package com.msm.core.objects.dto;

import com.msm.core.filter.domain.pageable.PageRequest;
import lombok.Data;

import java.util.Map;

/**
 * {
 *   "query": "order_summary",
 *   "params": {
 *     "status": "PAID",
 *     "minItemCount": 2,
 *     "fromDate": "2025-01-01",
 *     "toDate": "2025-12-31"
 *   },
 *   "pageRequest": {
 *     "sort": [
 *       { "field": "createdAt", "direction": "DESC" }
 *     ],
 *     "page": 1,
 *     "size": 20
 *   }
 * }
 */
@Data
public class QueryTemplate {
    private String query;
    private Map<String, Object> parameters;
    private PageRequest pageRequest;
}
