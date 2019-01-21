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

	public static String decodeSegment(String segment) {
		try {
			// Replacing + in URL to %2B because of this bug: https://bugs.openjdk.java.net/browse/JDK-8179507
			return URLDecoder.decode(segment.replaceAll("\\+", "%2B"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
