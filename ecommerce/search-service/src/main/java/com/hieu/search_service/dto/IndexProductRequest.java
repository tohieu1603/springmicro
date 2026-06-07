package com.hieu.search_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexProductRequest {
    private String id;
    private String name;
    private String description;
    private String sku;
    private String categoryId;
    private String categoryName;
    private String brand;
    private Double price;
    private Double minPrice;
    private Double maxPrice;
    private Integer totalStock;
    private String status;
    private String imageUrl;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
