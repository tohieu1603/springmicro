package com.hieu.user_profile_service.grpc;

import com.hieu.user_profile_service.entity.AddressJpaEntity;
import com.hieu.user_profile_service.entity.UserProfileJpaEntity;
import com.hieu.user_profile_service.repository.AddressRepository;
import com.hieu.user_profile_service.repository.UserProfileRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@GrpcService
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // all gRPC read methods — avoids dirty reads and enables connection-pool hints
public class UserProfileGrpcService extends UserProfileServiceGrpc.UserProfileServiceImplBase {

    private final UserProfileRepository profileRepo;
    private final AddressRepository addressRepo;

    @Override
    public void getProfile(GetProfileRequest request, StreamObserver<GetProfileResponse> responseObserver) {
        Optional<UserProfileJpaEntity> opt = profileRepo.findById(request.getUserId());
        GetProfileResponse.Builder builder = GetProfileResponse.newBuilder();
        if (opt.isPresent()) {
            UserProfileJpaEntity e = opt.get();
            builder.setFound(true)
                    .setUserId(e.getUserId())
                    .setEmail(nullSafe(e.getEmail()))
                    .setPhone(nullSafe(e.getPhone()))
                    .setFirstName(nullSafe(e.getFirstName()))
                    .setLastName(nullSafe(e.getLastName()))
                    .setAvatarUrl(nullSafe(e.getAvatarUrl()));
        } else {
            builder.setFound(false);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getEmail(GetEmailRequest request, StreamObserver<GetEmailResponse> responseObserver) {
        Optional<UserProfileJpaEntity> opt = profileRepo.findById(request.getUserId());
        GetEmailResponse.Builder builder = GetEmailResponse.newBuilder();
        if (opt.isPresent()) {
            builder.setFound(true).setEmail(nullSafe(opt.get().getEmail()));
        } else {
            builder.setFound(false);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAddress(GetAddressRequest request, StreamObserver<GetAddressResponse> responseObserver) {
        Optional<AddressJpaEntity> opt = addressRepo.findByIdAndUserProfile_UserId(
                request.getAddressId(), request.getUserId());
        GetAddressResponse.Builder builder = GetAddressResponse.newBuilder();
        if (opt.isPresent()) {
            AddressJpaEntity a = opt.get();
            builder.setFound(true)
                    .setId(a.getId())
                    .setRecipientName(nullSafe(a.getRecipientName()))
                    .setRecipientPhone(nullSafe(a.getRecipientPhone()))
                    .setStreet(nullSafe(a.getStreet()))
                    .setWard(nullSafe(a.getWard()))
                    .setDistrict(nullSafe(a.getDistrict()))
                    .setCity(nullSafe(a.getCity()))
                    .setCountry(nullSafe(a.getCountry()))
                    .setPostalCode(nullSafe(a.getPostalCode()));
        } else {
            builder.setFound(false);
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
