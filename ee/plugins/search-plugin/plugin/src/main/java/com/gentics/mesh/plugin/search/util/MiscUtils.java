package com.gentics.mesh.plugin.search.util;

import org.jsoup.Jsoup;

/**
 * Miscellaneous helper methods.
 */
public final class MiscUtils {

	/**
	 * Removes all HTML tags from the input text.
	 * 
	 * @param html The original markup
	 * @return The input string stripped of all HTML markup
	 */
	public static String stripHtml(String html) {
		if (html == null) {
			return null;
		}

		return Jsoup.parse(html).text();
	}
}
