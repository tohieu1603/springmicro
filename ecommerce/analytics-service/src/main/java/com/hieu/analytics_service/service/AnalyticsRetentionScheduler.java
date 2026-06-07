package com.hieu.analytics_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Daily retention sweep — delete analytics-events-YYYY.MM.dd indices older than retention-days.
 * Idempotent: DELETE INDEX is a no-op when the index is absent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsRetentionScheduler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final ElasticsearchTemplate esTemplate;

    @Value("${analytics.index-prefix:analytics-events}")
    private String indexPrefix;

    @Value("${analytics.retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "${analytics.retention-cron:0 30 2 * * *}") // 02:30 daily
    public void cleanupOldIndices() {
        LocalDate cutoff = LocalDate.now().minusDays(retentionDays);
        log.info("Analytics retention sweep started, cutoff={}", cutoff);
        int deleted = 0;
        // Walk from cutoff back to 365 days — covers realistic Elasticsearch index age range
        for (int back = retentionDays; back < 365; back++) {
            LocalDate d = LocalDate.now().minusDays(back);
            String name = indexPrefix + "-" + DATE_FMT.format(d);
            try {
                IndexOperations ops = esTemplate.indexOps(IndexCoordinates.of(name));
                if (ops.exists() && ops.delete()) {
                    deleted++;
                    log.info("Deleted old analytics index {}", name);
                }
            } catch (Exception e) {
                log.warn("Failed to delete index {}: {}", name, e.getMessage());
            }
        }
        log.info("Analytics retention sweep completed, deleted={}", deleted);
    }
}
