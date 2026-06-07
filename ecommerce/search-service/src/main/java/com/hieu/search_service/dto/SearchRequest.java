package com.hieu.search_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String q;
    private String brand;
    private String categoryId;
    private Double minPrice;
    private Double maxPrice;
    /** ACTIVE, INACTIVE, etc. */
    private String status;
    /** field name to sort by, default "createdAt" */
    private String sortBy;
    /** Cap at 99 to keep from*size <= ES default index.max_result_window=10000. */
    @Builder.Default
    @Min(0) @Max(99)
    private int page = 0;
    @Builder.Default
    @Min(1) @Max(100)
    private int size = 20;
}
