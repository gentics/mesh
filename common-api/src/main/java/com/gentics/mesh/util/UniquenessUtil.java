package com.gentics.mesh.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

/**
 * Utility which managed uniqueness checks for filenames.
 */
public final class UniquenessUtil {

	public static Pattern p = Pattern.compile("^(.*)_([0-9]{1,2})$");

	/**
	 * Suggest a new filename. Any found number postfix in the basename (excl. extension) will be incremented.
	 * 
	 * @param filename
	 * @return
	 */
	public static String suggestNewFilename(String filename) {
		String base = FilenameUtils.getBaseName(filename);
		String ext = FilenameUtils.getExtension(filename);
		String subname = suggestNewName(base);

		if (ext != null && !ext.isEmpty()) {
			return subname + "." + ext;
		} else {
			if (filename.endsWith(".")) {
				subname += ".";
			}
			return subname;
		}

	}

	/**
	 * Suggest a new name. Any found number postfix in the name will be incremented.
	 * 
	 * @param name
	 * @return
	 */
	public static String suggestNewName(String name) {

		Integer number = new Integer(1);

		Matcher m = p.matcher(name);
		boolean matchFound = m.find();

		if (matchFound) {
			name = m.group(1);
			number = Integer.parseInt(m.group(2));
			number++;
		}

		return name + "_" + number;
	}

}
