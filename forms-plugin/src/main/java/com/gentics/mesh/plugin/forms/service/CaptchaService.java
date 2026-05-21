package com.gentics.mesh.plugin.forms.service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;

import io.reactivex.Completable;

@Singleton
public class CaptchaService {
	private final List<CaptchaProvider> providers;

	@Inject
	public CaptchaService(FriendlyCaptchaProvider friendlyCaptcha, ReCaptchaProvider reCaptcha) {
		this.providers = List.of(friendlyCaptcha, reCaptcha);
	}

	/**
	 * Validate the captcha token for the given request context.
	 * Skipped when no captchaId is set, no matching provider exists, or this is an update (formDataNode already present).
	 */
	public Completable validate(FormDataCreateContext ctx) {
		String captchaId = ctx.getForm().getCaptchaId();
		if (StringUtils.isBlank(captchaId)) {
			return Completable.complete();
		}
		if (ctx.getFormDataNode() != null) {
			return Completable.complete();
		}
		String provider = captchaId.contains("_") ? captchaId.substring(0, captchaId.indexOf('_')) : captchaId;
		return providers.stream()
				.filter(p -> p.supports(provider))
				.findFirst()
				.map(p -> p.validate(ctx))
				.orElse(Completable.complete());
	}
}
