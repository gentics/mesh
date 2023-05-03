package com.gentics.mesh.core.rest.node.field;

/**
 * Status for binary field checks.
 *
 * <p>
 *     When a check service URL is specified, a binary fields initial check
 *     status is {@code POSTPONED} until the service confirms that the binary
 *     should be {@code ACCEPTED}.
 * </p>
 *
 * <p>
 *     Binary fields with a check status other than {@code ACCEPTED} will be
 *     treated as if they are not there.
 * </p>
 */
public enum BinaryCheckStatus {
	/** Binary checks are disabled, or the check service confirmed the binary as {@code ACCEPTED}. */
	ACCEPTED,
	/** The check service denied the binary. */
	DENIED,
	/** The check from the check service is still running. */
	POSTPONED
}
