package com.gentics.mesh.plugin.forms.config;

import java.util.Map;
import java.util.Set;

/**
 * File Upload Options
 */
public class FileUploadOptions {
	/**
	 * Default for creating thumbnails
	 */
	public static final boolean DEFAULT_CREATE_THUMBNAILS = true;

	/**
	 * Default width for creating thumbnails
	 */
	public static final int DEFAULT_THUMBNAIL_WIDTH = 350;

	/**
	 * Default height for creating thumbnails
	 */
	public static final int DEFAULT_THUMBNAIL_HEIGHT = 200;

	private Map<String, Set<String>> allowedFileTypes;

	private Map<String, Set<String>> allowedFileNames;

	private Set<String> defaultAllowed;

	private boolean useMagicByteCheck;

	private boolean createThumbnails = DEFAULT_CREATE_THUMBNAILS;

	private int thumbnailWidth = DEFAULT_THUMBNAIL_WIDTH;

	private int thumbnailHeight = DEFAULT_THUMBNAIL_HEIGHT;

	/**
	 * Get the allowed file types
	 * @return allowed file types
	 */
	public Map<String, Set<String>> getAllowedFileTypes() {
		return allowedFileTypes;
	}

	/**
	 * Set the allowed file types
	 * @param allowedFileTypes allowed file types
	 */
	public void setAllowedFileTypes(Map<String, Set<String>> allowedFileTypes) {
		this.allowedFileTypes = allowedFileTypes;
	}

	/**
	 * Get allowed file names
	 * @return allowed file names
	 */
	public Map<String, Set<String>> getAllowedFileNames() {
		return allowedFileNames;
	}

	/**
	 * Set the allowed file names
	 * @param allowedFileNames allowed file names
	 */
	public void setAllowedFileNames(Map<String, Set<String>> allowedFileNames) {
		this.allowedFileNames = allowedFileNames;
	}

	/**
	 * Categories of filenames or filetypes, which are allowed by default (null to allow everything by default)
	 * @return set of categories
	 */
	public Set<String> getDefaultAllowed() {
		return defaultAllowed;
	}

	/**
	 * Set categories which are allowed by default
	 * @param defaultAllowed categories
	 */
	public void setDefaultAllowed(Set<String> defaultAllowed) {
		this.defaultAllowed = defaultAllowed;
	}

	/**
	 * Whether to check a files magic bytes to verify the MIME type.
	 * @return {@code true} if the magic bytes must be verified.
	 */
	public boolean isUseMagicByteCheck() {
		return useMagicByteCheck;
	}

	/**
	 * Set whether to check a files magic bytes to verify the MIME type.
	 * @param useMagicByteCheck {@code true} if the magic bytes must be verified.
	 */
	public FileUploadOptions setUseMagicByteCheck(boolean useMagicByteCheck) {
		this.useMagicByteCheck = useMagicByteCheck;

		return this;
	}

	/**
	 * Whether thumbnails for uploaded images should be created
	 * @return true for creating thumbnails
	 */
	public boolean isCreateThumbnails() {
		return createThumbnails;
	}

	/**
	 * Set whether thumbnails should be created
	 * @param createThumbnails flag
	 * @return fluent API
	 */
	public FileUploadOptions setCreateThumbnails(boolean createThumbnails) {
		this.createThumbnails = createThumbnails;
		return this;
	}

	/**
	 * Get the thumbnail width
	 * @return width
	 */
	public int getThumbnailWidth() {
		return thumbnailWidth;
	}

	/**
	 * Set the thumbnail width
	 * @param thumbnailWidth width
	 * @return fluent API
	 */
	public FileUploadOptions setThumbnailWidth(int thumbnailWidth) {
		this.thumbnailWidth = thumbnailWidth;
		return this;
	}

	/**
	 * Get the thumbnail height
	 * @return height
	 */
	public int getThumbnailHeight() {
		return thumbnailHeight;
	}

	/**
	 * Set the thumbnail height
	 * @param thumbnailHeight height
	 * @return fluent API
	 */
	public FileUploadOptions setThumbnailHeight(int thumbnailHeight) {
		this.thumbnailHeight = thumbnailHeight;
		return this;
	}
}
