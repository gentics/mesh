package com.gentics.mesh.util;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.verticle.ObjectSchemaVerticle;

public final class UUIDUtil {

	private static final Logger log = LoggerFactory.getLogger(ObjectSchemaVerticle.class);

	private static  Pattern p = Pattern.compile("^[A-Fa-f0-9]+$");

	private UUIDUtil() {

	}

	public static boolean isUUID(String text) {
		// TODO maybe use regex here?
		// 
		if (text == null || text.length() != 32) {
			return false;
		} else {
			return p.matcher(text).matches();
		}
//		try {
//			if (com.fasterxml.uuid.impl.UUIDUtil.uuid(text) != null) {
//				return true;
//			}
//		} catch (NumberFormatException e) {
//			if (log.isDebugEnabled()) {
//				log.debug("Could not parse uuid {" + text + "}.", e);
//			}
//		}
	}
}
