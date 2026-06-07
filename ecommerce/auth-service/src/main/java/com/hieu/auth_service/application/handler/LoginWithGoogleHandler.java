package com.hieu.auth_service.application.handler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hieu.auth_service.application.command.LoginWithGoogleCommand;
import com.hieu.auth_service.application.common.CommandHandler;
import com.hieu.auth_service.application.dto.AuthResponseDTO;
import com.hieu.auth_service.application.mapper.UserDtoMapper;
import com.hieu.auth_service.domain.models.refreshtoken.RefreshToken;
import com.hieu.auth_service.domain.models.role.Role;
import com.hieu.auth_service.domain.models.role.vo.RoleName;
import com.hieu.auth_service.domain.models.user.User;
import com.hieu.auth_service.domain.models.user.exceptions.OAuthEmailNotVerifiedException;
import com.hieu.auth_service.domain.models.user.vo.Email;
import com.hieu.auth_service.domain.models.user.vo.GoogleSub;
import com.hieu.auth_service.domain.models.user.vo.PersonName;
import com.hieu.auth_service.domain.models.user.vo.Username;
import com.hieu.auth_service.domain.repositories.RefreshTokenRepository;
import com.hieu.auth_service.domain.repositories.RoleRepository;
import com.hieu.auth_service.domain.repositories.UserRepository;
import com.hieu.auth_service.domain.services.GoogleIdTokenVerifierPort;
import com.hieu.auth_service.domain.services.GoogleIdTokenVerifierPort.GoogleClaims;
import com.hieu.auth_service.domain.services.PasswordEncoderPort;
import com.hieu.auth_service.domain.services.TokenDomainService;
import com.hieu.auth_service.domain.services.TokenProviderPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles {@link LoginWithGoogleCommand}.
 *
 * <p>Verifies the Google ID token, then either:
 * <ul>
 *   <li>looks up the user by Google {@code sub} (preferred — sub is immutable),</li>
 *   <li>falls back to email lookup (links Google onto an existing password account), or</li>
 *   <li>auto-registers a new account with a derived username and a random
 *       unguessable password placeholder.</li>
 * </ul>
 * Step-up checks and password validation are intentionally bypassed: Google has
 * already authenticated the user (stronger signal than our own password).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginWithGoogleHandler implements CommandHandler<LoginWithGoogleCommand, AuthResponseDTO> {

    // Must match a role actually seeded by DataSeeder. ROLE_USER is the baseline role granted to
    // every self-registered account (password or Google) — an unseeded "ROLE_CUSTOMER" silently
    // left Google-registered users with no role at all.
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final int MAX_USERNAME_SUFFIX_TRIES = 1000;

    private final GoogleIdTokenVerifierPort googleVerifier;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final TokenDomainService tokenDomainService;
    private final UserDtoMapper userDtoMapper;

    @Value("${jwt.refresh-expiration-days:7}")
    private int refreshExpiryDays;

    @Override
    @Transactional
    public AuthResponseDTO handle(LoginWithGoogleCommand command) {
        GoogleClaims claims = googleVerifier.verify(command.idToken());
        if (!claims.emailVerified()) {
            throw new OAuthEmailNotVerifiedException();
        }

        // Lookup order: google_sub → email. sub is preferred because it stays
        // stable when Workspace users change their primary email.
        
        User user = userRepository.findByGoogleSub(GoogleSub.of(claims.sub()))
                .or(() -> userRepository.findByEmail(Email.of(claims.email())))
                .map(found -> {
                    // Existing password account → backfill the Google link on first OAuth login.
                    found.linkGoogleAccount(GoogleSub.of(claims.sub()));
                    return found;
                })
                .orElseGet(() -> registerNewGoogleUser(claims));

        user.ensureAuthenticatable();

        User saved = userRepository.save(user);
        List<Role> roles = roleRepository.findByIdIn(saved.getRoles());
        Set<String> roleNames = roles.stream()
                .map(r -> r.getName().value())
                .collect(Collectors.toSet());

        var issued = tokenProvider.issueAccessToken(saved, roleNames);
        RefreshToken refresh = tokenDomainService.issueForUser(saved, refreshExpiryDays);
        refreshTokenRepository.save(refresh);

        log.info("Google login success userId={} sub={}", saved.getId().value(), claims.sub());

        return AuthResponseDTO.bearer(
                issued.token(),
                refresh.getValue().value(),
                issued.expiresInSeconds(),
                userDtoMapper.toDto(saved, roles));
    }

    private User registerNewGoogleUser(GoogleClaims claims) {
        Username username = deriveUniqueUsername(claims);
        PersonName personName = derivePersonName(claims);
        Email email = Email.of(claims.email());
        GoogleSub sub = GoogleSub.of(claims.sub());

        User user = User.registerFromGoogle(username, email, personName, sub, passwordEncoder);

        // Attach default customer role. We look it up rather than hardcoding the
        // RoleId to support seed environments where role ids vary.
        roleRepository.findByName(RoleName.of(DEFAULT_ROLE))
                .ifPresent(role -> user.assignRole(role.getId()));

        return user;
    }

    /**
     * Picks a unique username derived from the Google profile. Strategy:
     * <ol>
     *   <li>sanitised email local-part (alice@hieu.vn → "alice")</li>
     *   <li>then alice1, alice2, … up to 1000 — bounded so a contention storm
     *       can't hang the request thread.</li>
     * </ol>
     */
    private Username deriveUniqueUsername(GoogleClaims claims) {
        String base = sanitiseUsername(claims.email());
        if (!userRepository.isUsernameTaken(safeUsername(base))) {
            return Username.of(base);
        }
        for (int i = 1; i < MAX_USERNAME_SUFFIX_TRIES; i++) {
            String candidate = base + i;
            if (!userRepository.isUsernameTaken(safeUsername(candidate))) {
                return Username.of(candidate);
            }
        }
        // Extremely improbable in practice; fall back to a uuid-suffixed name.
        return Username.of(base + "_" + java.util.UUID.randomUUID().toString().substring(0, 8));
    }

    /** Reduces an email or arbitrary string to a Username-shape candidate. */
    private static String sanitiseUsername(String email) {
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String cleaned = local.replaceAll("[^a-zA-Z0-9_-]", "");
        if (cleaned.length() < 3) cleaned = cleaned + "user";
        if (cleaned.length() > 50) cleaned = cleaned.substring(0, 50);
        return cleaned;
    }

    /** Wraps a candidate string into a Username VO without throwing on shape errors. */
    private static Username safeUsername(String candidate) {
        try {
            return Username.of(candidate);
        } catch (IllegalArgumentException ex) {
            // Forces the caller to fall through the next iteration.
            return Username.of(java.util.UUID.randomUUID().toString().substring(0, 8) + "x");
        }
    }

    /** Builds a PersonName, falling back to safe defaults when Google omits parts. */
    private static PersonName derivePersonName(GoogleClaims claims) {
        String given = blankToDefault(claims.givenName(), "Google");
        String family = blankToDefault(claims.familyName(), "User");
        // If name is provided as a single string and given/family are blank,
        // split on whitespace once.
        if ((isBlank(claims.givenName()) || isBlank(claims.familyName()))
                && !isBlank(claims.name()) && claims.name().contains(" ")) {
            String[] parts = claims.name().trim().split("\\s+", 2);
            given = isBlank(claims.givenName())  ? parts[0] : given;
            family = isBlank(claims.familyName()) ? parts[1] : family;
        }
        return PersonName.of(given, family);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String blankToDefault(String s, String fallback) {
        return isBlank(s) ? fallback : s.trim();
    }
}
