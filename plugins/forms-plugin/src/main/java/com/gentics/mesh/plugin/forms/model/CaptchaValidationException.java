package com.gentics.mesh.plugin.forms.model;

import io.vertx.core.json.JsonObject;

public class CaptchaValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String detail;

	public CaptchaValidationException(String detail) {
		super("Captcha validation failed: " + detail);
		this.detail = detail;
	}

	public String toResponseBody() {
		return new JsonObject()
			.put("error_code", "CAPTCHA_ERROR")
			.put("detail", detail)
			.encode();
	}
}
