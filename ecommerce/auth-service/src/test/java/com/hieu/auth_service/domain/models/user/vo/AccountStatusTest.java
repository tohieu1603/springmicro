package com.hieu.auth_service.domain.models.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccountStatus VO (unit)")
class AccountStatusTest {

    @Test
    @DisplayName("createActive() is active across all four flags")
    void active() {
        AccountStatus s = AccountStatus.createActive();
        assertThat(s.isActive()).isTrue();
        assertThat(s.lastLogin()).isNull();
    }

    @Test
    @DisplayName("lock()/unlock() flip accountNonLocked immutably")
    void lockUnlock() {
        AccountStatus active = AccountStatus.createActive();

        AccountStatus locked = active.lock();
        assertThat(locked.accountNonLocked()).isFalse();
        assertThat(locked.isActive()).isFalse();
        assertThat(active.accountNonLocked()).isTrue(); // original unchanged

        assertThat(locked.unlock().isActive()).isTrue();
    }

    @Test
    @DisplayName("disable()/enable() flip the enabled flag")
    void disableEnable() {
        AccountStatus disabled = AccountStatus.createActive().disable();
        assertThat(disabled.enabled()).isFalse();
        assertThat(disabled.isActive()).isFalse();

        assertThat(disabled.enable().isActive()).isTrue();
    }

    @Test
    @DisplayName("withLastLogin() records the timestamp without affecting status flags")
    void withLastLogin() {
        Instant now = Instant.now();

        AccountStatus s = AccountStatus.createActive().withLastLogin(now);

        assertThat(s.lastLogin()).isEqualTo(now);
        assertThat(s.isActive()).isTrue();
    }
}
