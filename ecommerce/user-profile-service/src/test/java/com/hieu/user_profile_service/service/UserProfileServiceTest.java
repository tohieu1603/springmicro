package com.hieu.user_profile_service.service;

import com.hieu.user_profile_service.dto.AddressDTO;
import com.hieu.user_profile_service.dto.UpdateProfileRequest;
import com.hieu.user_profile_service.dto.UpsertAddressRequest;
import com.hieu.user_profile_service.dto.UserProfileDTO;
import com.hieu.user_profile_service.entity.AddressJpaEntity;
import com.hieu.user_profile_service.entity.UserProfileJpaEntity;
import com.hieu.user_profile_service.exception.AddressNotFoundException;
import com.hieu.user_profile_service.exception.UserProfileNotFoundException;
import com.hieu.user_profile_service.kafka.event.ProfileUpsertedSpringEvent;
import com.hieu.user_profile_service.repository.AddressRepository;
import com.hieu.user_profile_service.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link UserProfileService} — partial profile update + event, lazy
 * profile provisioning on address create, single-default enforcement, and ownership lookups.
 * Repositories and the event publisher are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService (unit)")
class UserProfileServiceTest {

    private static final String USER = "user-1";

    @Mock UserProfileRepository profileRepo;
    @Mock AddressRepository addressRepo;
    @Mock ApplicationEventPublisher publisher;

    UserProfileService service;

    @BeforeEach
    void setup() {
        service = new UserProfileService(profileRepo, addressRepo, publisher);
    }

    private static UserProfileJpaEntity profile(String userId) {
        var e = new UserProfileJpaEntity();
        e.setUserId(userId);
        e.setEmail(userId + "@example.com");
        e.setFirstName("First");
        e.setLastName("Last");
        return e;
    }

    private static AddressJpaEntity address(String id, UserProfileJpaEntity owner) {
        var a = new AddressJpaEntity();
        a.setId(id);
        a.setUserProfile(owner);
        a.setLabel("Home");
        a.setRecipientName("Recipient");
        a.setRecipientPhone("0900000000");
        a.setStreet("123 Street");
        a.setCity("HCM");
        a.setCountry("Vietnam");
        return a;
    }

    private static UpsertAddressRequest addrRequest(boolean isDefault) {
        var r = new UpsertAddressRequest();
        r.setLabel("Office");
        r.setRecipientName("Recipient");
        r.setRecipientPhone("0900000000");
        r.setStreet("456 Road");
        r.setCity("HCM");
        r.setDefault(isDefault);
        return r;
    }

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("returns the mapped profile when present")
        void found() {
            when(profileRepo.findById(USER)).thenReturn(Optional.of(profile(USER)));
            UserProfileDTO dto = service.getProfile(USER);
            assertThat(dto.getUserId()).isEqualTo(USER);
            assertThat(dto.getFirstName()).isEqualTo("First");
        }

        @Test
        @DisplayName("throws when the profile is missing")
        void notFound() {
            when(profileRepo.findById(USER)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getProfile(USER))
                    .isInstanceOf(UserProfileNotFoundException.class);
        }

        @Test
        @DisplayName("getProfileByEmail throws when not found")
        void byEmailNotFound() {
            when(profileRepo.findByEmail("x@example.com")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getProfileByEmail("x@example.com"))
                    .isInstanceOf(UserProfileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("applies only non-null fields and publishes a profile-upserted event")
        void partialUpdate() {
            when(profileRepo.findById(USER)).thenReturn(Optional.of(profile(USER)));
            when(profileRepo.save(any(UserProfileJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            var req = new UpdateProfileRequest();
            req.setFirstName("Updated");          // only firstName supplied

            UserProfileDTO dto = service.updateProfile(USER, req);

            assertThat(dto.getFirstName()).isEqualTo("Updated");
            assertThat(dto.getLastName()).isEqualTo("Last"); // untouched
            verify(publisher).publishEvent(any(ProfileUpsertedSpringEvent.class));
        }
    }

    @Nested
    @DisplayName("createAddress()")
    class CreateAddress {

        @Test
        @DisplayName("lazily provisions a missing profile and clears the old default")
        void lazyProvisionAndDefault() {
            when(profileRepo.findById(USER)).thenReturn(Optional.empty());
            when(profileRepo.save(any(UserProfileJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(addressRepo.save(any(AddressJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            AddressDTO dto = service.createAddress(USER, addrRequest(true));

            assertThat(dto.getUserId()).isEqualTo(USER);
            assertThat(dto.isDefault()).isTrue();
            verify(profileRepo).save(any(UserProfileJpaEntity.class)); // backfilled
            verify(addressRepo).clearDefaultForUser(USER);
        }

        @Test
        @DisplayName("does not clear the default when the new address is not default")
        void nonDefaultDoesNotClear() {
            when(profileRepo.findById(USER)).thenReturn(Optional.of(profile(USER)));
            when(addressRepo.save(any(AddressJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            AddressDTO dto = service.createAddress(USER, addrRequest(false));

            assertThat(dto.isDefault()).isFalse();
            verify(addressRepo, never()).clearDefaultForUser(any());
        }
    }

    @Nested
    @DisplayName("updateAddress() / setDefaultAddress() / deleteAddress()")
    class AddressMutations {

        @Test
        @DisplayName("updateAddress sets the new default and applies fields")
        void update_setsDefault() {
            var existing = address("5", profile(USER));
            when(addressRepo.findByIdAndUserProfile_UserId("5", USER)).thenReturn(Optional.of(existing));
            when(addressRepo.save(any(AddressJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            AddressDTO dto = service.updateAddress(USER, "5", addrRequest(true));

            assertThat(dto.isDefault()).isTrue();
            assertThat(dto.getLabel()).isEqualTo("Office");
            verify(addressRepo).clearDefaultForUser(USER);
        }

        @Test
        @DisplayName("updateAddress throws for an address the user does not own")
        void update_notFound() {
            when(addressRepo.findByIdAndUserProfile_UserId("5", USER)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateAddress(USER, "5", addrRequest(false)))
                    .isInstanceOf(AddressNotFoundException.class);
        }

        @Test
        @DisplayName("setDefaultAddress clears the previous default and marks the new one")
        void setDefault() {
            when(profileRepo.findById(USER)).thenReturn(Optional.of(profile(USER)));
            var existing = address("5", profile(USER));
            when(addressRepo.findByIdAndUserProfile_UserId("5", USER)).thenReturn(Optional.of(existing));
            when(addressRepo.save(any(AddressJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            AddressDTO dto = service.setDefaultAddress(USER, "5");

            assertThat(dto.isDefault()).isTrue();
            verify(addressRepo).clearDefaultForUser(USER);
        }

        @Test
        @DisplayName("deleteAddress removes the owned address")
        void delete() {
            var existing = address("5", profile(USER));
            when(addressRepo.findByIdAndUserProfile_UserId("5", USER)).thenReturn(Optional.of(existing));

            service.deleteAddress(USER, "5");

            verify(addressRepo).delete(eq(existing));
        }
    }
}
