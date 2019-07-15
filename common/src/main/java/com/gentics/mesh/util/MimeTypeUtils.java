package com.gentics.mesh.util;

import java.util.Optional;

import io.vertx.core.http.impl.MimeMapping;

public class MimeTypeUtils {

	/** Default MIME type for binary files. */
	public static final String DEFAULT_BINARY_MIME_TYPE = "application/octet-stream";

	/**
	 * Try to determine the MIME type from the given filename.
	 *
	 * The actual work is done by the {@link MimeMapping#getMimeTypeForFilename MimeMapping} class, this wrapper takes care of <code>null</code> values.
	 *
	 * @see MimeMapping#getMimeTypeForFilename(String)
	 * @param filename The filename to get a MIME type for
	 * @return An empty Optional when the filename is <code>null</code> or no MIME type could be determined, or an Optional containing the MIME type otherwise.
	 */
	public static Optional<String> getMimeTypeForFilename(String filename) {
		return Optional.ofNullable(filename)
			.map(MimeMapping::getMimeTypeForFilename);
	}
}
