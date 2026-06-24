package com.gentics.mesh.plugin.forms.model;
// for formgen load response
public class CaptchaConfigModel {

	private String provider;
	private String mode;
	private String sitekey;

	public CaptchaConfigModel(String provider, String mode, String sitekey) {
		this.provider = provider;
		this.mode = mode;
		this.sitekey = sitekey;
	}

	public String getProvider() {
		return provider;
	}

	public String getMode() {
		return mode;
	}

	public String getSitekey() {
		return sitekey;
	}
}
