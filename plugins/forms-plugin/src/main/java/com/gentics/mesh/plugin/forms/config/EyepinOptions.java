package com.gentics.mesh.plugin.forms.config;

/**
 * Eyepin Options
 */
public class EyepinOptions {

	private final String DEFAULT_API_URL = "https://apiv3.eyepin.com/interface3.php";

	private String username;

	private String password;

	private String apiUrl = DEFAULT_API_URL;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getApiUrl() {
		if (apiUrl == null || apiUrl.isEmpty()) {
			apiUrl = DEFAULT_API_URL;
		}
		return apiUrl;
	}

	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}
}
