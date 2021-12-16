package com.gentics.mesh.plugin.forms.config;

/**
 * FriendlyCaptcha Options
 */
public class FriendlyCaptchaOptions {

	private final String DEFAULT_SOLUTION_KEY = "frc-captcha-solution";

	private final String DEFAULT_VERIFICATION_URL = "https://api.friendlycaptcha.com/api/v1/siteverify";

	private String siteKey;

	private String secretKey;

	private String solutionKey = DEFAULT_SOLUTION_KEY;

	private String verificationUrl = DEFAULT_VERIFICATION_URL;

	private boolean acceptOnErrorStatus = false;

	public String getSiteKey() {
		return siteKey;
	}

	public void setSiteKey(String siteKey) {
		this.siteKey = siteKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getVerificationUrl() {
		if(verificationUrl == null || verificationUrl.isEmpty()) {
			verificationUrl = DEFAULT_VERIFICATION_URL;
		}
		return verificationUrl;
	}

	public void setVerificationUrl(String verificationUrl) {
		this.verificationUrl = verificationUrl;
	}

	public String getSolutionKey() {
		if(solutionKey == null || solutionKey.isEmpty()) {
			solutionKey = DEFAULT_SOLUTION_KEY;
		}
		return solutionKey;
	}

	public void setSolutionKey(String solutionKey) {
		this.solutionKey = solutionKey;
	}

	public Boolean getAcceptOnErrorStatus() {
		return acceptOnErrorStatus;
	}

	public void setAcceptOnErrorStatus(Boolean acceptOnErrorStatus) {
		this.acceptOnErrorStatus = acceptOnErrorStatus;
	}
}
