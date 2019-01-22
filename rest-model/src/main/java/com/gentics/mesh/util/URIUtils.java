package com.gentics.mesh.util;

import com.google.common.net.UrlEscapers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public final class URIUtils {

	private URIUtils() { }
	/**
	 * Encode the URL path segment. This method will also encode '?' and '/' characters in the provided segment.
	 * 
	 * @param segment
	 * @return
	 */
	public static String encodeSegment(String segment) {
		return UrlEscapers.urlPathSegmentEscaper().escape(segment);
	}

	/**
	 * Decode the URL path segment.
	 * This uses {@link URLDecoder#decode(String, String)} to decode the string, because the above method {@link #encodeSegment(String)}
	 * does not have its own corresponding decode method.
	 *
	 * When decoding in this way, "+" in the segment are replaced correctly.
	 * A special implementation is necessary because of this bug: https://bugs.openjdk.java.net/browse/JDK-8179507
	 *
	 * @param segment
	 * @return
	 */
	public static String decodeSegment(String segment) {
		try {
			// replaceAll makes sure that "+" in the segment are not replaced with spaces
			return URLDecoder.decode(segment.replaceAll("\\+", "%2B"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
