package com.hieu.analytics_service.document;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Single analytics event indexed into Elasticsearch (index pattern: analytics-events-*).
 * No @Document annotation — index is selected dynamically per write to enable date-based rolling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventDocument {
    private String eventType;
    private String userId;
    private String referenceId;
    private String referenceType;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Map<String, Object> metadata;
    private Instant timestamp;
}
