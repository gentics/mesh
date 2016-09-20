package com.gentics.mesh.util;

import com.google.common.net.UrlEscapers;

public final class URIUtils {

	/**
	 * Encode the url fragment
	 * 
	 * @param value
	 * @return
	 */
	public static String encodeFragment(String value) {
		String partiallyEncodedFragment =  UrlEscapers.urlFragmentEscaper().escape(value);
		partiallyEncodedFragment = partiallyEncodedFragment.replaceAll("\\?", "%3F");
		return partiallyEncodedFragment;
	}

}
