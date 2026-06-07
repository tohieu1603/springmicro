package com.hieu.shipping_service.controller;

import com.hieu.common.api.ApiResponse;
import com.hieu.shipping_service.dto.CalculateFeeRequest;
import com.hieu.shipping_service.dto.CalculateFeeResponse;
import com.hieu.shipping_service.entity.CarrierConfigEntity;
import com.hieu.shipping_service.repository.CarrierConfigRepository;
import com.hieu.shipping_service.service.GhtkClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link ShippingFeeController}: fee delegation, the enabled/sorted
 * carrier projections, the toMap shape, and the upsert + selective-patch logic in
 * updateCarrier. Repo + GhtkClient mocked; no Spring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShippingFeeController (unit)")
class ShippingFeeControllerTest {

    @Mock GhtkClient ghtkClient;
    @Mock CarrierConfigRepository carrierRepo;
    @InjectMocks ShippingFeeController controller;

    private static CarrierConfigEntity carrier(String code, boolean enabled, int order) {
        var e = new CarrierConfigEntity();
        e.setCode(code);
        e.setName(code + " Name");
        e.setEnabled(enabled);
        e.setSupportsCod(true);
        e.setEtaHours(48);
        e.setDisplayOrder(order);
        return e;
    }

    @Test
    @DisplayName("calculateFee delegates to GhtkClient and wraps the quote")
    void calculateFee() {
        var req = new CalculateFeeRequest("HCM", "D1", "W1", "addr", 1000, 0L, null);
        var quote = CalculateFeeResponse.ghtk(30_000L, 1_000L, 24L);
        when(ghtkClient.calculateFee(req)).thenReturn(quote);

        var resp = controller.calculateFee(req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().data()).isEqualTo(quote);
        assertThat(resp.getBody().data().source()).isEqualTo("GHTK_LIVE");
    }

    @Test
    @DisplayName("carriers() returns only enabled carriers, sorted by displayOrder")
    void carriers_enabledSorted() {
        when(carrierRepo.findAll()).thenReturn(List.of(
                carrier("GHN", true, 30),
                carrier("DISABLED", false, 5),
                carrier("GHTK", true, 10)));

        var resp = controller.carriers();

        List<Map<String, Object>> rows = resp.getBody().data();
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("code")).isEqualTo("GHTK"); // order 10 first
        assertThat(rows.get(1).get("code")).isEqualTo("GHN");  // order 30 second
        assertThat(rows).noneMatch(r -> "DISABLED".equals(r.get("code")));
    }

    @Test
    @DisplayName("carriers() toMap exposes the full carrier shape")
    void carriers_toMapShape() {
        when(carrierRepo.findAll()).thenReturn(List.of(carrier("GHTK", true, 10)));

        var row = controller.carriers().getBody().data().get(0);

        assertThat(row).containsOnlyKeys("code", "name", "enabled", "supportsCod", "etaHours", "displayOrder");
        assertThat(row.get("code")).isEqualTo("GHTK");
        assertThat(row.get("name")).isEqualTo("GHTK Name");
        assertThat(row.get("enabled")).isEqualTo(true);
        assertThat(row.get("supportsCod")).isEqualTo(true);
        assertThat(row.get("etaHours")).isEqualTo(48);
        assertThat(row.get("displayOrder")).isEqualTo(10);
    }

    @Test
    @DisplayName("adminListCarriers() returns disabled carriers too, sorted")
    void adminListCarriers_includesDisabled() {
        when(carrierRepo.findAll()).thenReturn(List.of(
                carrier("GHN", true, 30),
                carrier("DISABLED", false, 5)));

        var rows = controller.adminListCarriers().getBody().data();

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("code")).isEqualTo("DISABLED"); // order 5 first
        assertThat(rows.get(1).get("code")).isEqualTo("GHN");
    }

    @Test
    @DisplayName("updateCarrier patches only the supplied fields on an existing carrier")
    void updateCarrier_patchExisting() {
        var existing = carrier("GHTK", true, 10);
        existing.setName("Old Name");
        when(carrierRepo.findById("GHTK")).thenReturn(Optional.of(existing));
        when(carrierRepo.save(any(CarrierConfigEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> body = new HashMap<>();
        body.put("name", "New Name");
        body.put("enabled", false);
        body.put("etaHours", 72);
        // supportsCod + displayOrder omitted -> unchanged

        ApiResponse<Map<String, Object>> resp = controller.updateCarrier("GHTK", body).getBody();

        ArgumentCaptor<CarrierConfigEntity> captor = ArgumentCaptor.forClass(CarrierConfigEntity.class);
        verify(carrierRepo).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("New Name");
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getEtaHours()).isEqualTo(72);
        assertThat(saved.isSupportsCod()).isTrue();       // unchanged
        assertThat(saved.getDisplayOrder()).isEqualTo(10); // unchanged
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(resp.data().get("name")).isEqualTo("New Name");
    }

    @Test
    @DisplayName("updateCarrier creates a new carrier when code is unknown")
    void updateCarrier_upsertNew() {
        when(carrierRepo.findById("NEWCO")).thenReturn(Optional.empty());
        when(carrierRepo.save(any(CarrierConfigEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> body = new HashMap<>();
        body.put("displayOrder", 7);

        controller.updateCarrier("NEWCO", body);

        ArgumentCaptor<CarrierConfigEntity> captor = ArgumentCaptor.forClass(CarrierConfigEntity.class);
        verify(carrierRepo).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("NEWCO");
        assertThat(saved.getName()).isEqualTo("NEWCO"); // defaulted to code
        assertThat(saved.getDisplayOrder()).isEqualTo(7);
    }

    @Test
    @DisplayName("updateCarrier with an empty body keeps defaults and still saves")
    void updateCarrier_emptyBody() {
        when(carrierRepo.findById("GHTK")).thenReturn(Optional.of(carrier("GHTK", true, 10)));
        when(carrierRepo.save(any(CarrierConfigEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        controller.updateCarrier("GHTK", new HashMap<>());

        verify(carrierRepo).save(any(CarrierConfigEntity.class));
    }

    @Test
    @DisplayName("updateCarrier coerces numeric etaHours/displayOrder from Number")
    void updateCarrier_numericCoercion() {
        when(carrierRepo.findById("GHTK")).thenReturn(Optional.of(carrier("GHTK", true, 10)));
        when(carrierRepo.save(any(CarrierConfigEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> body = new HashMap<>();
        body.put("etaHours", 100L);       // Long
        body.put("displayOrder", 3.0);    // Double

        controller.updateCarrier("GHTK", body);

        ArgumentCaptor<CarrierConfigEntity> captor = ArgumentCaptor.forClass(CarrierConfigEntity.class);
        verify(carrierRepo).save(captor.capture());
        assertThat(captor.getValue().getEtaHours()).isEqualTo(100);
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("carriers() does not touch GhtkClient")
    void carriers_noGhtkCall() {
        when(carrierRepo.findAll()).thenReturn(List.of());
        controller.carriers();
        verify(ghtkClient, never()).calculateFee(any());
    }
}
