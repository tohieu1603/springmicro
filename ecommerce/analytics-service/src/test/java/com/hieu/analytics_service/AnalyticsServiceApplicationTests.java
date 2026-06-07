package com.hieu.analytics_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.elasticsearch.uris=http://localhost:9200",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
        })
@ActiveProfiles("test")
class AnalyticsServiceApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test — Elasticsearch + Kafka excluded to avoid infra deps
    }
}
