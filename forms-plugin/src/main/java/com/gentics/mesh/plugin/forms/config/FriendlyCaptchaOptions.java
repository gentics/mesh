package com.gentics.mesh.plugin.forms.config;

import com.gentics.mesh.plugin.forms.model.FriendlyCaptchaElement;

/**
 * FriendlyCaptcha Options
 */
public class FriendlyCaptchaOptions {
	private String siteKey;

	private String secretKey;

	private String solutionKey = FriendlyCaptchaElement.SOLUTION_KEY;

	private String verificationUrl = FriendlyCaptchaElement.VERIFICATION_URL;

	private Boolean acceptOnErrorStatus = false;

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
		return verificationUrl;
	}

	public void setVerificationUrl(String verificationUrl) {
		this.verificationUrl = verificationUrl;
	}

	public String getSolutionKey() {
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
