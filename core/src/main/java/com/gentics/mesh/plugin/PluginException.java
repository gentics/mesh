package com.gentics.mesh.plugin;

/**
 * Generic exception for plugins.
 */
public class PluginException extends RuntimeException {

	private static final long serialVersionUID = 8210841231803284375L;

	public PluginException(String msg) {
		super(msg);
	}
}
