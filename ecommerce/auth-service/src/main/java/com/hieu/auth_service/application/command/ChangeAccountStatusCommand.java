package com.hieu.auth_service.application.command;

import com.hieu.auth_service.application.common.Command;

/**
 * Transitions a user's account-status flag (admin-only use case).
 *
 * <p>A single command with an enum discriminator replaces four near-identical
 * commands (lock/unlock/enable/disable), keeping the CQRS surface compact.
 *
 * @param userId     target user id
 * @param transition desired status transition
 */
public record ChangeAccountStatusCommand(String userId, Transition transition)
        implements Command<Void> {

    /** Supported account-status transitions. */
    public enum Transition { LOCK, UNLOCK, ENABLE, DISABLE }
}
