package com.hieu.auth_service.infrastructure.security;

import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter that bridges the domain {@link PasswordEncoderPort} onto
 * whatever Spring Security {@link PasswordEncoder} bean is wired (typically BCrypt).
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final PasswordEncoder delegate;

    public BCryptPasswordEncoderAdapter(PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
