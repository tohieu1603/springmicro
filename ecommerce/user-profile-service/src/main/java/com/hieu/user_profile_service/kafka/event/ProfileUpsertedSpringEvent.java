package com.hieu.user_profile_service.kafka.event;

/** Internal Spring application event; published after DB commit, consumed by {@link com.hieu.user_profile_service.kafka.listener.ProfileUpsertedListener}. */
public record ProfileUpsertedSpringEvent(String userId, String email, String firstName, String lastName, String phone) {}
