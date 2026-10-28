package com.gentics.mesh.plugin.forms.model.schema;

import java.util.Set;

public class BinaryValidation {
	private Integer maxFilesize;

	private Set<String> allowedTypes;

	private Set<String> allowedFileTypes;

	private Set<String> allowedFileNames;

	/**
	 * Get the maximum allowed filesize (in MB) for binary elements
	 * @return maximum allowed filesize
	 */
	public Integer getMaxFilesize() {
		return maxFilesize;
	}

	/**
	 * Set the maximum allowed filesize (in MB) for binary elements
	 * @param maxFilesize maximum allowed filesize
	 * @return fluent API
	 */
	public BinaryValidation setMaxFilesize(Integer maxFilesize) {
		this.maxFilesize = maxFilesize;

		return this;
	}

	/**
	 * Get the allowed types
	 * @return allowed types
	 */
	public Set<String> getAllowedTypes() {
		return allowedTypes;
	}

	/**
	 * Set the allowed types
	 * @param allowedTypes allowed types
	 * @return fluent API
	 */
	public BinaryValidation setAllowedTypes(Set<String> allowedTypes) {
		this.allowedTypes = allowedTypes;
		return this;
	}

	/**
	 * Get allowed file type regexes
	 * @return allowed file types
	 */
	public Set<String> getAllowedFileTypes() {
		return allowedFileTypes;
	}

	/**
	 * Set the allowed file types
	 * @param allowedFileTypes file types
	 * @return fluent API
	 */
	public BinaryValidation setAllowedFileTypes(Set<String> allowedFileTypes) {
		this.allowedFileTypes = allowedFileTypes;
		return this;
	}

	/**
	 * Get the allowed file name regexes
	 * @return allowed file names
	 */
	public Set<String> getAllowedFileNames() {
		return allowedFileNames;
	}

	/**
	 * Set the allowed file names
	 * @param allowedFileNames allowed file names
	 * @return fluent API
	 */
	public BinaryValidation setAllowedFileNames(Set<String> allowedFileNames) {
		this.allowedFileNames = allowedFileNames;
		return this;
	}
}
