package com.hieu.user_profile_service.grpc;

import com.hieu.user_profile_service.entity.AddressJpaEntity;
import com.hieu.user_profile_service.entity.UserProfileJpaEntity;
import com.hieu.user_profile_service.repository.AddressRepository;
import com.hieu.user_profile_service.repository.UserProfileRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link UserProfileGrpcService}: the JPA-entity -> protobuf-response
 * mapping plus the found / not-found branches and the {@code nullSafe} empty-string coercion.
 * Repositories are mocked; the {@link StreamObserver} is mocked and its emitted response captured.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileGrpcService (unit)")
class UserProfileGrpcServiceTest {

    @Mock UserProfileRepository profileRepo;
    @Mock AddressRepository addressRepo;
    @InjectMocks UserProfileGrpcService grpc;

    private static UserProfileJpaEntity profile() {
        var e = new UserProfileJpaEntity();
        e.setUserId("u-1");
        e.setEmail("a@test.com");
        e.setPhone("0900");
        e.setFirstName("Ann");
        e.setLastName("Lee");
        e.setAvatarUrl("http://img/a.png");
        return e;
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("maps every entity field into the response and sets found=true")
        void found() {
            when(profileRepo.findById("u-1")).thenReturn(Optional.of(profile()));
            @SuppressWarnings("unchecked")
            StreamObserver<GetProfileResponse> obs = org.mockito.Mockito.mock(StreamObserver.class);

            grpc.getProfile(GetProfileRequest.newBuilder().setUserId("u-1").build(), obs);

            ArgumentCaptor<GetProfileResponse> captor = ArgumentCaptor.forClass(GetProfileResponse.class);
            verify(obs).onNext(captor.capture());
            verify(obs).onCompleted();

            GetProfileResponse r = captor.getValue();
            assertThat(r.getFound()).isTrue();
            assertThat(r.getUserId()).isEqualTo("u-1");
            assertThat(r.getEmail()).isEqualTo("a@test.com");
            assertThat(r.getPhone()).isEqualTo("0900");
            assertThat(r.getFirstName()).isEqualTo("Ann");
            assertThat(r.getLastName()).isEqualTo("Lee");
            assertThat(r.getAvatarUrl()).isEqualTo("http://img/a.png");
        }

        @Test
        @DisplayName("null entity fields are coerced to empty strings, not propagated as null")
        void nullFieldsCoercedToEmpty() {
            var e = new UserProfileJpaEntity();
            e.setUserId("u-2");
            e.setEmail("only@email.com");
            // phone/firstName/lastName/avatarUrl remain null
            when(profileRepo.findById("u-2")).thenReturn(Optional.of(e));
            @SuppressWarnings("unchecked")
            StreamObserver<GetProfileResponse> obs = org.mockito.Mockito.mock(StreamObserver.class);

            grpc.getProfile(GetProfileRequest.newBuilder().setUserId("u-2").build(), obs);

            ArgumentCaptor<GetProfileResponse> captor = ArgumentCaptor.forClass(GetProfileResponse.class);
            verify(obs).onNext(captor.capture());
            GetProfileResponse r = captor.getValue();
            assertThat(r.getFound()).isTrue();
            assertThat(r.getPhone()).isEmpty();
            assertThat(r.getFirstName()).isEmpty();
            assertThat(r.getLastName()).isEmpty();
            assertThat(r.getAvatarUrl()).isEmpty();
        }

        @Test
        @DisplayName("missing profile -> found=false and no other fields set")
        void notFound() {
            when(profileRepo.findById("missing")).thenReturn(Optional.empty());
            @SuppressWarnings("unchecked")
            StreamObserver<GetProfileResponse> obs = org.mockito.Mockito.mock(StreamObserver.class);

            grpc.getProfile(GetProfileRequest.newBuilder().setUserId("missing").build(), obs);

            ArgumentCaptor<GetProfileResponse> captor = ArgumentCaptor.forClass(GetProfileResponse.class);
            verify(obs).onNext(captor.capture());
            verify(obs).onCompleted();
            GetProfileResponse r = captor.getValue();
            assertThat(r.getFound()).isFalse();
            assertThat(r.getUserId()).isEmpty();
            assertThat(r.getEmail()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getEmail")
    class GetEmail {

        @Test
        @DisplayName("present profile -> found=true with the email")
        void found() {
            when(profileRepo.findById("u-1")).thenReturn(Optional.of(profile()));
            @SuppressWarnings("unchecked")
            StreamObserver<GetEmailResponse> obs = org.mockito.Mockito.mock(StreamObserver.class);

            grpc.getEmail(GetEmailRequest.newBuilder().setUserId("u-1").build(), obs);

            ArgumentCaptor<GetEmailResponse> captor = ArgumentCaptor.forClass(GetEmailResponse.class);
            verify(obs).onNext(captor.capture());
            verify(obs).onCompleted();
            assertThat(captor.getValue().getFound()).isTrue();
            assertThat(captor.getValue().getEmail()).isEqualTo("a@test.com");
        }

        @Test
        @DisplayName("missing profile -> found=false and empty email")
        void notFound() {
            when(profileRepo.findById("missing")).thenReturn(Optional.empty());
            @SuppressWarnings("unchecked")
            StreamObserver<GetEmailResponse> obs = org.mockito.Mockito.mock(StreamObserver.class);

            grpc.getEmail(GetEmailRequest.newBuilder().setUserId("missing").build(), obs);

            ArgumentCaptor<GetEmailResponse> captor = ArgumentCaptor.forClass(GetEmailResponse.class);
            verify(obs).onNext(captor.capture());
            assertThat(captor.getValue().getFound()).isFalse();
            assertThat(captor.getValue().getEmail()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAddress")
    class GetAddress {

        private static AddressJpaEntity address() {
            var a = new AddressJpaEntity();
            a.setId("55");
            a.setRecipientName("Bob");
            a.setRecipientPhone("0911");
            a.setStreet("12 Main");
            a.setWard("W1");
            a.setDistrict("D2");
            a.setCity("HCM");
            a.setCountry("Vietnam");
            a.setPostalCode("70000");
            return a;
        }

        @Test
        @DisplayName("found by (addressId, userId) -> maps all fields with found=true")
        void found() {
            when(addressRepo.findByIdAndUserProfile_UserId("55", "u-1"))
                    .thenReturn(Optional.of(address()));
            @SuppressWarnings("unchecked")
            StreamObserver<GetAddressResponse> obs = org.mockito.Mockito.mock(StreamObserver.class);

            grpc.getAddress(GetAddressRequest.newBuilder().setUserId("u-1").setAddressId("55").build(), obs);

            ArgumentCaptor<GetAddressResponse> captor = ArgumentCaptor.forClass(GetAddressResponse.class);
            verify(obs).onNext(captor.capture());
            verify(obs).onCompleted();
            GetAddressResponse r = captor.getValue();
            assertThat(r.getFound()).isTrue();
            assertThat(r.getId()).isEqualTo("55");
            assertThat(r.getRecipientName()).isEqualTo("Bob");
            assertThat(r.getRecipientPhone()).isEqualTo("0911");
            assertThat(r.getStreet()).isEqualTo("12 Main");
            assertThat(r.getWard()).isEqualTo("W1");
            assertThat(r.getDistrict()).isEqualTo("D2");
            assertThat(r.getCity()).isEqualTo("HCM");
            assertThat(r.getCountry()).isEqualTo("Vietnam");
            assertThat(r.getPostalCode()).isEqualTo("70000");
        }

        @Test
        @DisplayName("null optional address fields coerce to empty strings")
        void nullFieldsCoerced() {
            var a = new AddressJpaEntity();
            a.setId("56");
            a.setRecipientName("Cy");
            a.setRecipientPhone("0922");
            a.setStreet("9 Side");
            a.setCity("HN");
            a.setCountry("Vietnam");
            a.setWard(null);
            a.setDistrict(null);
            a.setPostalCode(null);
            when(addressRepo.findByIdAndUserProfile_UserId("56", "u-1")).thenReturn(Optional.of(a));
            @SuppressWarnings("unchecked")
            StreamObserver<GetAddressResponse> obs = org.mockito.Mockito.mock(StreamObserver.class);

            grpc.getAddress(GetAddressRequest.newBuilder().setUserId("u-1").setAddressId("56").build(), obs);

            ArgumentCaptor<GetAddressResponse> captor = ArgumentCaptor.forClass(GetAddressResponse.class);
            verify(obs).onNext(captor.capture());
            GetAddressResponse r = captor.getValue();
            assertThat(r.getWard()).isEmpty();
            assertThat(r.getDistrict()).isEmpty();
            assertThat(r.getPostalCode()).isEmpty();
        }

        @Test
        @DisplayName("not found -> found=false and id defaults to 0")
        void notFound() {
            when(addressRepo.findByIdAndUserProfile_UserId("99", "u-1")).thenReturn(Optional.empty());
            @SuppressWarnings("unchecked")
            StreamObserver<GetAddressResponse> obs = org.mockito.Mockito.mock(StreamObserver.class);

            grpc.getAddress(GetAddressRequest.newBuilder().setUserId("u-1").setAddressId("99").build(), obs);

            ArgumentCaptor<GetAddressResponse> captor = ArgumentCaptor.forClass(GetAddressResponse.class);
            verify(obs).onNext(captor.capture());
            verify(obs).onCompleted();
            assertThat(captor.getValue().getFound()).isFalse();
            assertThat(captor.getValue().getId()).isEmpty();
        }
    }
}
