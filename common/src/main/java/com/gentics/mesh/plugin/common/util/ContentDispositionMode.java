package com.gentics.mesh.plugin.common.util;

import org.apache.commons.lang.StringUtils;

/**
 * Values for setting the Content-Disposition HTTP header.
 */
public enum ContentDispositionMode {

	/** Send all binaries with {@code Content-Disposition: inline}. */
	INLINE,
	/** Send all binaries with {@code Content-Disposition: attachment[, filename="filename-if-available"]}. */
	ATTACHMENT,
	/** Send images as {@code inline} and all other binaries as {@code attachment}. */
	SMART;

	private static final String VALUE_INLINE = "inline";
	private static final String VALUE_ATTACHMENT = "attachment";

	/**
	 * Get a Content-Disposition HTTP header value according to this enum value.
	 *
	 * @param contentType The Content-Type of the binary
	 * @param filename The binaries filename (optional)
	 * @return The Content-Disposition header according to this value
	 */
	String contentDisposition(String contentType, String filename) {
		switch (this) {
		case INLINE:
			return VALUE_INLINE;

		case ATTACHMENT:
			return attachment(filename);

		case SMART:
		default:
			return contentType.startsWith("image/")
				? VALUE_INLINE
				: attachment(filename);
		}
	}

	/**
	 * Get a Content-Disposition header with {@code attachment} and the
	 * filename if present.
	 *
	 * @param filename The filename of the binary (optional)
	 * @return A Content-Disposition header with {@code attachment}
	 */
	String attachment(String filename) {
		return StringUtils.isBlank(filename)
			? VALUE_ATTACHMENT
			: String.format("%s; filename=\"%s\"", VALUE_ATTACHMENT, filename);
	}
}
