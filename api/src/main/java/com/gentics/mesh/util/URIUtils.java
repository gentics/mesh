package com.gentics.mesh.util;

import com.google.common.net.UrlEscapers;

public final class URIUtils {

	/**
	 * Encode the url fragment. This method will also encode '?' and '/' characters in the provided fragment.
	 * 
	 * @param fragment
	 * @return
	 */
	public static String encodeFragment(String fragment) {
		String partiallyEncodedFragment = UrlEscapers.urlFragmentEscaper().escape(fragment);
		partiallyEncodedFragment = partiallyEncodedFragment.replaceAll("\\?", "%3F");
		partiallyEncodedFragment = partiallyEncodedFragment.replaceAll("/", "%2F");
		return partiallyEncodedFragment;
	}

}
