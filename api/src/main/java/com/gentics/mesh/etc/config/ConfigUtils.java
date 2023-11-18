package com.gentics.mesh.etc.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mesh Options and Config utils.
 */
public final class ConfigUtils {

	public final static Pattern QUOTA_PATTERN_PERCENTAGE = Pattern.compile("(?<value>[0-9]{1,2})%");
	public final static Pattern QUOTA_PATTERN_SIZE = Pattern.compile("(?<value>[0-9]+)(?<unit>b|B|k|K|m|M|g|G|t|T)");
	public final static Pattern QUOTA_PATTERN_NUMBER = Pattern.compile("(?<value>[0-9]+)");

	private ConfigUtils() {
	}

	public static long getBytes(Matcher absoluteMatcher) {
		switch (absoluteMatcher.group("unit")) {
		case "b":
		case "B":
			return Long.parseLong(absoluteMatcher.group("value"));
		case "k":
		case "K":
			return Long.parseLong(absoluteMatcher.group("value")) * 1024;
		case "m":
		case "M":
			return Long.parseLong(absoluteMatcher.group("value")) * 1024 * 1024;
		case "g":
		case "G":
			return Long.parseLong(absoluteMatcher.group("value")) * 1024 * 1024 * 1024;
		case "t":
		case "T":
			return Long.parseLong(absoluteMatcher.group("value")) * 1024 * 1024 * 1024 * 1024;
		default:
			return 0;
		}
	}
}
