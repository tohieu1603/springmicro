package com.hieu.shipping_service.kafka;

import com.hieu.shipping_service.dto.CreateShipmentRequest;
import com.hieu.shipping_service.service.OrderServiceClient;
import com.hieu.shipping_service.service.ShipmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link PaymentCompletedConsumer} — the Kafka {@code payment.completed}
 * handler that fetches the order's shipping address from order-service and auto-creates a
 * shipment. {@link ShipmentService} and {@link OrderServiceClient} are mocked; no Spring,
 * no Kafka, no DB. Focuses on the consumer's branching + its private extractMap/str/strOrDefault
 * mapping into a {@link CreateShipmentRequest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCompletedConsumer (unit)")
class PaymentCompletedConsumerTest {

    @Mock ShipmentService shipmentService;
    @Mock OrderServiceClient orderServiceClient;

    PaymentCompletedConsumer consumer;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        consumer = new PaymentCompletedConsumer(shipmentService, orderServiceClient);
    }

    private static PaymentCompletedEvent event() {
        return new PaymentCompletedEvent("evt-1", "ORD-9", "user-42", "COMPLETED");
    }

    private static Map<String, Object> fullAddress() {
        var addr = new HashMap<String, Object>();
        addr.put("recipientName", "Nguyen Van A");
        addr.put("recipientPhone", "0901234567");
        addr.put("addressLine", "123 Le Loi");
        addr.put("ward", "Ward 1");
        addr.put("district", "District 1");
        addr.put("city", "Ho Chi Minh");
        addr.put("country", "Vietnam");
        return addr;
    }

    @Test
    @DisplayName("maps the shippingAddress map into a CreateShipmentRequest and creates the shipment")
    void happyPath_buildsRequestAndCreates() {
        var order = new HashMap<String, Object>();
        order.put("shippingAddress", fullAddress());
        when(orderServiceClient.fetchOrder("ORD-9")).thenReturn(Optional.of(order));

        consumer.onPaymentCompleted(event());

        var captor = ArgumentCaptor.forClass(CreateShipmentRequest.class);
        verify(shipmentService).createShipmentIfAbsent(captor.capture());
        CreateShipmentRequest req = captor.getValue();

        assertThat(req.orderId()).isEqualTo("ORD-9");
        assertThat(req.userId()).isEqualTo("user-42");
        assertThat(req.carrier()).isNull();
        assertThat(req.recipientName()).isEqualTo("Nguyen Van A");
        assertThat(req.recipientPhone()).isEqualTo("0901234567");
        assertThat(req.addressLine()).isEqualTo("123 Le Loi");
        assertThat(req.ward()).isEqualTo("Ward 1");
        assertThat(req.district()).isEqualTo("District 1");
        assertThat(req.city()).isEqualTo("Ho Chi Minh");
        assertThat(req.country()).isEqualTo("Vietnam");
        assertThat(req.notes()).isNull();
    }

    @Test
    @DisplayName("defaults city to 'Unknown' and country to 'Vietnam' when absent")
    void defaultsCityAndCountryWhenMissing() {
        var addr = new HashMap<String, Object>();
        addr.put("recipientName", "Recipient");
        addr.put("recipientPhone", "0900000000");
        addr.put("addressLine", "1 Some Street");
        // no city, no country
        var order = new HashMap<String, Object>();
        order.put("shippingAddress", addr);
        when(orderServiceClient.fetchOrder("ORD-9")).thenReturn(Optional.of(order));

        consumer.onPaymentCompleted(event());

        var captor = ArgumentCaptor.forClass(CreateShipmentRequest.class);
        verify(shipmentService).createShipmentIfAbsent(captor.capture());
        CreateShipmentRequest req = captor.getValue();

        assertThat(req.city()).isEqualTo("Unknown");
        assertThat(req.country()).isEqualTo("Vietnam");
        // optional fields not present map to null
        assertThat(req.ward()).isNull();
        assertThat(req.district()).isNull();
    }

    @Test
    @DisplayName("defaults city/country when present but blank")
    void defaultsCityAndCountryWhenBlank() {
        var addr = fullAddress();
        addr.put("city", "   ");
        addr.put("country", "");
        var order = new HashMap<String, Object>();
        order.put("shippingAddress", addr);
        when(orderServiceClient.fetchOrder("ORD-9")).thenReturn(Optional.of(order));

        consumer.onPaymentCompleted(event());

        var captor = ArgumentCaptor.forClass(CreateShipmentRequest.class);
        verify(shipmentService).createShipmentIfAbsent(captor.capture());
        CreateShipmentRequest req = captor.getValue();

        assertThat(req.city()).isEqualTo("Unknown");
        assertThat(req.country()).isEqualTo("Vietnam");
    }

    @Test
    @DisplayName("stringifies non-String address values via toString()")
    void stringifiesNonStringValues() {
        var addr = new HashMap<String, Object>();
        addr.put("recipientName", "Recipient");
        addr.put("recipientPhone", 84901234567L); // numeric value
        addr.put("addressLine", "1 Some Street");
        addr.put("city", "HCM");
        var order = new HashMap<String, Object>();
        order.put("shippingAddress", addr);
        when(orderServiceClient.fetchOrder("ORD-9")).thenReturn(Optional.of(order));

        consumer.onPaymentCompleted(event());

        var captor = ArgumentCaptor.forClass(CreateShipmentRequest.class);
        verify(shipmentService).createShipmentIfAbsent(captor.capture());
        assertThat(captor.getValue().recipientPhone()).isEqualTo("84901234567");
    }

    @Test
    @DisplayName("order-service unavailable (empty Optional) → no shipment created")
    void orderServiceUnavailable_noShipment() {
        when(orderServiceClient.fetchOrder("ORD-9")).thenReturn(Optional.empty());

        consumer.onPaymentCompleted(event());

        verify(shipmentService, never()).createShipmentIfAbsent(any());
    }

    @Test
    @DisplayName("order has no shippingAddress key → skipped, no shipment created")
    void noShippingAddressKey_skipped() {
        var order = new HashMap<String, Object>();
        order.put("orderId", "ORD-9"); // present but no shippingAddress
        when(orderServiceClient.fetchOrder("ORD-9")).thenReturn(Optional.of(order));

        consumer.onPaymentCompleted(event());

        verify(shipmentService, never()).createShipmentIfAbsent(any());
    }

    @Test
    @DisplayName("shippingAddress present but empty map → skipped, no shipment created")
    void emptyShippingAddress_skipped() {
        var order = new HashMap<String, Object>();
        order.put("shippingAddress", new HashMap<String, Object>());
        when(orderServiceClient.fetchOrder("ORD-9")).thenReturn(Optional.of(order));

        consumer.onPaymentCompleted(event());

        verify(shipmentService, never()).createShipmentIfAbsent(any());
    }

    @Test
    @DisplayName("shippingAddress present but wrong type (not a Map) → skipped")
    void shippingAddressWrongType_skipped() {
        var order = new HashMap<String, Object>();
        order.put("shippingAddress", "not-a-map");
        when(orderServiceClient.fetchOrder("ORD-9")).thenReturn(Optional.of(order));

        consumer.onPaymentCompleted(event());

        verify(shipmentService, never()).createShipmentIfAbsent(any());
    }
}
