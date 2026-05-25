package com.msm.core.objects.dto;

import com.msm.core.filter.domain.SearchRequest;
import com.msm.core.filter.domain.pageable.PageRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * <pre>
 * {
 *   "query": "order_summary",
 *   "parameters": {
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
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryTemplate {
    private String query;
    private SearchRequest search;
    private Map<String, Object> parameters;
    private PageRequest pageRequest;
}
