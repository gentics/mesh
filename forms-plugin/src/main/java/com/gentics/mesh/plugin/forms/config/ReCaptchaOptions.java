package com.gentics.mesh.plugin.forms.config;

import com.gentics.mesh.plugin.forms.model.ReCaptchaElement;

/**
 * reCAPTCHA options
 */
public class ReCaptchaOptions {
	private String siteKey;

	private String secretKey;

	private String verificationUrl = ReCaptchaElement.VERIFICATION_URL;

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
}
