package com.gentics.mesh.plugin.forms.service;

import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;

import io.reactivex.Completable;

public interface CaptchaProvider {

	/**
	 * Returns true if this provider handles the given captcha provider name (extracted from captchaId before '_').
	 * @param provider provider name
	 * @return true when the provider handles it
	 */
	boolean supports(String provider);

	/**
	 * Validate the captcha for the given request context.
	 * Completes normally if valid; errors with {@link com.gentics.mesh.plugin.forms.model.CaptchaValidationException} if the solution is invalid,
	 * or {@link IllegalStateException} for server-side configuration/network errors.
	 * @param configuration configuration
	 * @param ctx context
	 * @return completable
	 */
	Completable validate(FormsConfig configuration, FormDataCreateContext ctx);
}
