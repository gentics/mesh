package com.gentics.mesh.plugin.forms.model;

/**
 * Valid form types.
 */
public enum FormType {
	/**
	 * A generic form with no restrictions aside from those in the form schema,
	 * and no special behavior.
	 */
	GENERIC,

	/**
	 * The only allowed input fields are selection fields, recaptcha and submit.
	 * Render requests for polls will check if the current user already voted,
	 * and if so will render the results instead of the poll.
	 */
	POLL
}
