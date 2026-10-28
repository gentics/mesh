package com.gentics.mesh.plugin.forms.config;

/**
 * reCAPTCHA options
 */
public class ReCaptchaOptions {

	private final String DEFAULT_VERIFICATION_URL = "https://www.google.com/recaptcha/api/siteverify";
	private final String DEFAULT_RESPONSE_TOKEN_KEY = "x-recaptcha-token";
	private String siteKey;
	private String secretKey;
	private String responseTokenKey = DEFAULT_RESPONSE_TOKEN_KEY;
	private String verificationUrl = DEFAULT_VERIFICATION_URL;
	/**
	 * Site Key
	 * @return site key
	 */
	public String getSiteKey() {
		return siteKey;
	}

	/**
	 * Set the site key
	 * @param siteKey site key
	 */
	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	/**
	 * Secret key
	 * @return secret key
	 */
	public String getSecretKey() {
		return secretKey;
	}

	/**
	 * Set the secret key
	 * @param secretKey secret key
	 */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getVerificationUrl() {
		return verificationUrl;
	}

	public void setVerificationUrl(String verificationUrl) {
		this.verificationUrl = verificationUrl;
	}

	public String getResponseTokenKey() {
		return responseTokenKey;
	}

	public void setResponseTokenKey(String solutionKey) {
		this.responseTokenKey = solutionKey;
	}
}
