package com.gentics.mesh.plugin.forms.model.schema;

import java.util.Map;

public class RegexValidation {

	private String key;
	private String regex;
	private Map<String, String> errorMessage;

	/**
	 * Get the key of the validation.
	 *
	 * @return The key of the validation.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Set the key of the validation.
	 *
	 * @param key The key of the validation.
	 * @return Fluent API
	 */
	public RegexValidation setKey(String key) {
		this.key = key;
		return this;
	}

	/**
	 * Get the regex pattern.
	 *
	 * @return The regex pattern.
	 */
	public String getRegex() {
		return regex;
	}

	/**
	 * Set the regex pattern.
	 *
	 * @param regex The regex pattern.
	 * @return Fluent API
	 */
	public RegexValidation setRegex(String regex) {
		this.regex = regex;
		return this;
	}

	/**
	 * Get the error message translations.
	 * In the returned map the key is the language code, and the value is the translated error message.
	 *
	 * @return The error message translations.
	 */
	public Map<String, String> getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Set the error message translations.
	 *
	 * @param errorMessage The error message translations.
	 * @return Fluent API
	 */
	public RegexValidation setErrorMessage(Map<String, String> errorMessage) {
		this.errorMessage = errorMessage;
		return this;
	}
}
