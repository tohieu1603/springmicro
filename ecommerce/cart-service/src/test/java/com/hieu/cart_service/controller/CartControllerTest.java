package com.hieu.cart_service.controller;

import com.hieu.cart_service.dto.AddToCartRequest;
import com.hieu.cart_service.dto.CartDTO;
import com.hieu.cart_service.dto.UpdateCartItemRequest;
import com.hieu.cart_service.service.CartService;
import com.hieu.common.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link CartController}: the {@link CartService} is mocked and the test
 * asserts the controller extracts {@code userId} from the {@link AuthenticatedUser} principal,
 * delegates correctly and maps results to the right {@link ResponseEntity} status (200 vs 204).
 * No MockMvc / Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartController (unit)")
class CartControllerTest {

    @Mock CartService cartService;
    @InjectMocks CartController controller;

    private static final AuthenticatedUser USER =
            new AuthenticatedUser("u1", "alice", List.of("ROLE_USER"), List.of());

    private static CartDTO sampleCart() {
        return new CartDTO("u1", List.of(), 0, BigDecimal.ZERO, List.of());
    }

    @Test
    @DisplayName("getCart returns 200 with the cart for the authenticated user's id")
    void getCart() {
        var cart = sampleCart();
        when(cartService.getCart("u1")).thenReturn(cart);

        ResponseEntity<CartDTO> resp = controller.getCart(USER);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(cart);
        verify(cartService).getCart("u1");
    }

    @Test
    @DisplayName("addItem delegates the request under the user's id and returns 200")
    void addItem() {
        var req = new AddToCartRequest("10", "100", 2, "idem-1");
        var cart = sampleCart();
        when(cartService.addItem("u1", req)).thenReturn(cart);

        ResponseEntity<CartDTO> resp = controller.addItem(USER, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(cart);
        verify(cartService).addItem("u1", req);
    }

    @Test
    @DisplayName("updateItem passes the path variantId + body and returns 200")
    void updateItem() {
        var req = new UpdateCartItemRequest(5);
        var cart = sampleCart();
        when(cartService.updateItem("u1", "100", req)).thenReturn(cart);

        ResponseEntity<CartDTO> resp = controller.updateItem(USER, "100", req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(cart);
        verify(cartService).updateItem("u1", "100", req);
    }

    @Test
    @DisplayName("removeItem returns 204 No Content with no body and delegates the removal")
    void removeItem() {
        ResponseEntity<Void> resp = controller.removeItem(USER, "100");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(resp.getBody()).isNull();
        verify(cartService).removeItem("u1", "100");
        verifyNoMoreInteractions(cartService);
    }

    @Test
    @DisplayName("clearCart returns 204 No Content and clears the user's cart")
    void clearCart() {
        ResponseEntity<Void> resp = controller.clearCart(USER);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(resp.getBody()).isNull();
        verify(cartService).clearCart("u1");
        verifyNoMoreInteractions(cartService);
    }
}
