package com.gentics.mesh.etc.config;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Mesh Options and Config utils.
 */
public final class ConfigUtils {

	public final static Pattern QUOTA_PATTERN_PERCENTAGE = Pattern.compile("(?<value>[0-9]{1,2})%");
	public final static Pattern QUOTA_PATTERN_SIZE = Pattern.compile("(?<value>[0-9]+)(?<unit>b|B|k|K|m|M|g|G|t|T)");
	public final static Pattern QUOTA_PATTERN_NUMBER = Pattern.compile("(?<value>[0-9]+)");

	private ConfigUtils() {
	}

	/**
	 * Parse the given quota setting, which should match one of the quota patterns {@link #QUOTA_PATTERN_PERCENTAGE}, {@link #QUOTA_PATTERN_SIZE} or {@link #QUOTA_PATTERN_NUMBER}.
	 * @param setting setting to parse
	 * @param total total (100%) for calculating a percentage setting (in bytes)
	 * @param percentage consumer getting the absolute value in bytes calculated from a percentage setting
	 * @param size consumer getting the absolute memory size in bytes
	 * @param number consumer getting the absolute number
	 * @param other consumer for getting the setting, when it does not match any of the patterns
	 * @param empty callback for empty setting
	 */
	public static void parseQuotaSetting(String setting, Long total, Consumer<Long> percentage, Consumer<Long> size,
			Consumer<Long> number, Consumer<String> other, Runnable empty) {
		if (StringUtils.isNotBlank(setting)) {
			setting = setting.replace("_", "");
			Matcher percentageMatcher = ConfigUtils.QUOTA_PATTERN_PERCENTAGE.matcher(setting);
			Matcher sizeMatcher = ConfigUtils.QUOTA_PATTERN_SIZE.matcher(setting);
			Matcher numberMatcher = ConfigUtils.QUOTA_PATTERN_NUMBER.matcher(setting);
			if (percentageMatcher.matches()) {
				percentage.accept(total / 100L * Long.parseLong(percentageMatcher.group("value")));
			} else if (sizeMatcher.matches()) {
				size.accept(ConfigUtils.getBytes(sizeMatcher));
			} else if (numberMatcher.matches()) {
				number.accept(Long.parseLong(numberMatcher.group("value")));
			} else {
				other.accept(setting);
			}
		} else {
			empty.run();
		}
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
