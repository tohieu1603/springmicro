package com.hieu.user_profile_service.controller;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.user_profile_service.dto.AddressDTO;
import com.hieu.user_profile_service.dto.UpdateProfileRequest;
import com.hieu.user_profile_service.dto.UpsertAddressRequest;
import com.hieu.user_profile_service.dto.UserProfileDTO;
import com.hieu.user_profile_service.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link UserProfileController}: verifies the controller extracts the
 * principal's userId, forwards path variables, and maps service results to the right
 * HTTP status (201 CREATED on create, body passthrough elsewhere) without MockMvc or a
 * Spring context. The service is mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileController (unit)")
class UserProfileControllerTest {

    private static final String USER = "user-1";

    @Mock UserProfileService service;
    @InjectMocks UserProfileController controller;

    private static AuthenticatedUser principal(String userId) {
        return new AuthenticatedUser(userId, "alice", List.of("ROLE_USER"), List.of());
    }

    private static UserProfileDTO profileDTO(String userId) {
        return UserProfileDTO.builder().userId(userId).email(userId + "@x.com").build();
    }

    private static AddressDTO addressDTO(String id) {
        return AddressDTO.builder().id(id).userId(USER).city("HCM").build();
    }

    @Test
    @DisplayName("getMyProfile uses the principal's userId")
    void getMyProfile() {
        var dto = profileDTO(USER);
        when(service.getProfile(USER)).thenReturn(dto);

        UserProfileDTO result = controller.getMyProfile(principal(USER));

        assertThat(result).isSameAs(dto);
        verify(service).getProfile(USER);
    }

    @Test
    @DisplayName("updateMyProfile forwards the principal's userId and request body")
    void updateMyProfile() {
        var req = new UpdateProfileRequest();
        req.setFirstName("New");
        var dto = profileDTO(USER);
        when(service.updateProfile(USER, req)).thenReturn(dto);

        UserProfileDTO result = controller.updateMyProfile(principal(USER), req);

        assertThat(result).isSameAs(dto);
        verify(service).updateProfile(USER, req);
    }

    @Test
    @DisplayName("getMyAddresses returns the service list for the principal")
    void getMyAddresses() {
        var list = List.of(addressDTO("1"), addressDTO("2"));
        when(service.getAddresses(USER)).thenReturn(list);

        List<AddressDTO> result = controller.getMyAddresses(principal(USER));

        assertThat(result).isEqualTo(list);
    }

    @Test
    @DisplayName("createAddress returns 201 CREATED with the created address in the body")
    void createAddress_returns201() {
        var req = new UpsertAddressRequest();
        var dto = addressDTO("99");
        when(service.createAddress(USER, req)).thenReturn(dto);

        ResponseEntity<AddressDTO> resp = controller.createAddress(principal(USER), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isSameAs(dto);
        verify(service).createAddress(USER, req);
    }

    @Test
    @DisplayName("updateAddress forwards userId, addressId and body")
    void updateAddress() {
        var req = new UpsertAddressRequest();
        var dto = addressDTO("5");
        when(service.updateAddress(USER, "5", req)).thenReturn(dto);

        AddressDTO result = controller.updateAddress(principal(USER), "5", req);

        assertThat(result).isSameAs(dto);
        verify(service).updateAddress(USER, "5", req);
    }

    @Test
    @DisplayName("deleteAddress delegates to the service (NO_CONTENT is declared via @ResponseStatus)")
    void deleteAddress() {
        controller.deleteAddress(principal(USER), "8");

        verify(service).deleteAddress(USER, "8");
    }

    @Test
    @DisplayName("setDefault forwards userId and addressId")
    void setDefault() {
        var dto = addressDTO("3");
        when(service.setDefaultAddress(USER, "3")).thenReturn(dto);

        AddressDTO result = controller.setDefault(principal(USER), "3");

        assertThat(result).isSameAs(dto);
        verify(service).setDefaultAddress(USER, "3");
    }

    @Test
    @DisplayName("getProfileByAdmin looks up by the path userId, not the caller")
    void getProfileByAdmin() {
        var dto = profileDTO("other-user");
        when(service.getProfile("other-user")).thenReturn(dto);

        UserProfileDTO result = controller.getProfileByAdmin("other-user");

        assertThat(result).isSameAs(dto);
        verify(service).getProfile("other-user");
    }

    @Test
    @DisplayName("getByEmail looks up by the path email")
    void getByEmail() {
        var dto = profileDTO("u");
        when(service.getProfileByEmail("a@b.com")).thenReturn(dto);

        UserProfileDTO result = controller.getByEmail("a@b.com");

        assertThat(result).isSameAs(dto);
        verify(service).getProfileByEmail("a@b.com");
    }

    @Test
    @DisplayName("getAddressInternal (no-auth saga endpoint) forwards both path variables")
    void getAddressInternal() {
        var dto = addressDTO("11");
        when(service.getAddress("u-7", "11")).thenReturn(dto);

        AddressDTO result = controller.getAddressInternal("u-7", "11", principal("u-7"));

        assertThat(result).isSameAs(dto);
        verify(service).getAddress(eq("u-7"), eq("11"));
    }
}
