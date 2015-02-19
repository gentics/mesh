package com.gentics.cailun.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.core.verticle.TypesVerticle;

public final class UUIDUtil {

	private static final Logger log = LoggerFactory.getLogger(TypesVerticle.class);

	private UUIDUtil() {

	}

	public static boolean isUUID(String text) {
		// TODO maybe use regex here?
		try {
			if (com.fasterxml.uuid.impl.UUIDUtil.uuid(text) != null) {
				return true;
			}
		} catch (NumberFormatException e) {
			if (log.isDebugEnabled()) {
				log.debug("Could not parse uuid {" + text + "}.", e);
			}
		}
		return false;
	}
}
