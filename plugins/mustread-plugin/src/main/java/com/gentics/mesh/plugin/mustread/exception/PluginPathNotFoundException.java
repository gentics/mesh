package com.gentics.mesh.plugin.mustread.exception;

/**
 * Exception that tells us that the PluginPath could not be found
 */
public class PluginPathNotFoundException extends RuntimeException {
    public PluginPathNotFoundException(String message) {
        super(message);
    }
}
