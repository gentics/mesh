package com.gentics.mesh.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Logback converter which provides the instance codename within the log output.
 */
public class MeshLogNameConverter extends ClassicConverter {

	@Override
	public String convert(ILoggingEvent event) {

		// TODO Which node name to use in a multi tenancy setup
		// return Mesh.mesh().getOptions().getNodeName();
		return "";
	}
}
