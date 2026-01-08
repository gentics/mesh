package com.gentics.mesh.plugin.forms.model;

import com.github.jknack.handlebars.internal.lang3.StringUtils;

/**
 * Model of a validation error
 */
public class ValidationError {
	private String name;

	private String title;

	private String message;

	private boolean preventStoring = false;

	/**
	 * Name of the field
	 * @return field name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set field name
	 * @param name field name
	 * @return fluent API
	 */
	public ValidationError setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Title of the validation error
	 * @return title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set the title
	 * @param title title
	 * @return fluent API
	 */
	public ValidationError setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Message of the validation error
	 * @return message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the message
	 * @param message message
	 * @return fluent API
	 */
	public ValidationError setMessage(String message) {
		this.message = message;
		return this;
	}

	/**
	 * Check whether storing shall be prevented due to this error
	 * @return true to prevent storing
	 */
	public boolean isPreventStoring() {
		return preventStoring;
	}

	/**
	 * Set whether storing shall be prevented due to this error
	 * @param preventStoring true to prevent storing
	 * @return fluent API
	 */
	public ValidationError setPreventStoring(boolean preventStoring) {
		this.preventStoring = preventStoring;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Validation Error:");
		if (StringUtils.isNotBlank(title)) {
			sb.append(" ").append(title);
		}
		if (StringUtils.isNotBlank(name)) {
			sb.append(" in [").append(name).append("]");
		}
		if (StringUtils.isNotBlank(message)) {
			sb.append(": ").append(message);
		}
		return sb.toString();
	}
}
