package com.gentics.mesh.util;

import com.google.common.net.UrlEscapers;

public final class URIUtils {

	public static String encodeFragment(String value) {
		return UrlEscapers.urlFragmentEscaper().escape(value);
	}

}
