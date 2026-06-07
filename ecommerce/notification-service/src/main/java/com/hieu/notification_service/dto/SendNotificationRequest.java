package com.hieu.notification_service.dto;

import com.hieu.notification_service.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Request to send a single notification. Used by REST endpoint and Kafka consumers. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotBlank
    private String userId;

    @NotNull
    private NotificationType type;

    /** Required for EMAIL/SMS; nullable for IN_APP/PUSH. */
    private String channel;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private String referenceType;
    private String referenceId;
}
