package com.gentics.mesh.plugin.forms.model;

/**
 * Model of a validation error
 */
public class ValidationError {
	private String name;

	private String title;

	private String message;

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
}
