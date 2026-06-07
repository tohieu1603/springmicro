package com.hieu.flash_sale_service.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link FlashSaleEventPublisher}. The {@link KafkaTemplate} is mocked and
 * returns a completed (or failed) future; we assert the topic + partition-key derivation per event
 * type and that a broker failure is swallowed (logged, never re-thrown) since the tx already
 * committed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FlashSaleEventPublisher (unit)")
class FlashSaleEventPublisherTest {

    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    private FlashSaleEventPublisher publisher;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        publisher = new FlashSaleEventPublisher(kafkaTemplate);
    }

    private void stubSendOk() {
        CompletableFuture<SendResult<String, Object>> ok = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(ok);
    }

    @Test
    @DisplayName("onStarted -> sends to 'flashsale.started' keyed by saleId")
    void onStarted() {
        stubSendOk();
        var event = new FlashSaleStartedEvent("e1", Instant.now(), "55", "p1",
                Instant.now(), Instant.now().plusSeconds(60), 100);

        publisher.onStarted(event);

        verify(kafkaTemplate).send(KafkaTopics.FLASH_SALE_STARTED, "55", event);
    }

    @Test
    @DisplayName("onEnded -> sends to 'flashsale.ended' keyed by saleId")
    void onEnded() {
        stubSendOk();
        var event = new FlashSaleEndedEvent("e2", Instant.now(), "77", "p2", 12);

        publisher.onEnded(event);

        verify(kafkaTemplate).send(KafkaTopics.FLASH_SALE_ENDED, "77", event);
    }

    @Test
    @DisplayName("onSlotReserved -> sends to 'flashsale.slot-reserved' keyed by saleId")
    void onSlotReserved() {
        stubSendOk();
        var event = new FlashSaleSlotReservedEvent("e3", Instant.now(), "88", "u1", 2, 8);

        publisher.onSlotReserved(event);

        verify(kafkaTemplate).send(KafkaTopics.FLASH_SALE_SLOT_RESERVED, "88", event);
    }

    @Test
    @DisplayName("topic and key match the constants and event saleId for the started event")
    void capturesTopicAndKey() {
        stubSendOk();
        var event = new FlashSaleStartedEvent("e", Instant.now(), "123", "p",
                Instant.now(), Instant.now().plusSeconds(60), 10);

        publisher.onStarted(event);

        var topic = ArgumentCaptor.forClass(String.class);
        var key = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topic.capture(), key.capture(), eq(event));
        assertThat(topic.getValue()).isEqualTo("flashsale.started");
        assertThat(key.getValue()).isEqualTo("123");
    }

    @Test
    @DisplayName("a broker send failure is swallowed (never propagated to the caller)")
    void brokerFailure_swallowed() {
        var failed = new CompletableFuture<SendResult<String, Object>>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(failed);
        var event = new FlashSaleEndedEvent("e", Instant.now(), "1", "p", 0);

        assertThatCode(() -> publisher.onEnded(event)).doesNotThrowAnyException();
        verify(kafkaTemplate).send(eq(KafkaTopics.FLASH_SALE_ENDED), eq("1"), any());
    }
}
