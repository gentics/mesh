package com.gentics.mesh.util;

import com.google.common.net.UrlEscapers;

public final class URIUtils {

	/**
	 * Encode the URL path segment. This method will also encode '?' and '/' characters in the provided segment.
	 * 
	 * @param segment
	 * @return
	 */
	public static String encodeSegment(String segment) {
		return UrlEscapers.urlPathSegmentEscaper().escape(segment);
	}

}
