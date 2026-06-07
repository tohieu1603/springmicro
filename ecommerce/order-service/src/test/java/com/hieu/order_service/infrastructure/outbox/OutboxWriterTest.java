package com.hieu.order_service.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pure unit tests for the outbox row writer. */
@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    @Mock OutboxEventJpaRepository repository;

    @Test
    @DisplayName("append serializes the payload and persists a fully-populated row")
    void append_persistsRow() {
        var writer = new OutboxWriter(repository, new ObjectMapper());

        writer.append("Order", "42", "OrderPlaced", "order.events", Map.of("id", 42));

        var captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());
        var e = captor.getValue();
        assertThat(e.getAggregateType()).isEqualTo("Order");
        assertThat(e.getAggregateId()).isEqualTo("42");
        assertThat(e.getEventType()).isEqualTo("OrderPlaced");
        assertThat(e.getTopic()).isEqualTo("order.events");
        assertThat(e.getPayload()).contains("\"id\":42");
        assertThat(e.getRetryCount()).isEqualTo(0);
        assertThat(e.getCreatedAt()).isNotNull();
        assertThat(e.getNextAttemptAt()).isNotNull();
    }

    @Test
    @DisplayName("append wraps a serialization failure in a RuntimeException and never saves")
    void append_serializationFailure() throws JsonProcessingException {
        var failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new JsonProcessingException("bad") {});
        var writer = new OutboxWriter(repository, failingMapper);

        assertThatThrownBy(() -> writer.append("Order", "1", "T", "topic", new Object()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Outbox append failed");

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
