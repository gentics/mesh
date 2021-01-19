package com.gentics.mesh.etc.config.env;

/**
 * Container for environment test values
 */
public class ValueEntry {

	public ValueEntry(String stringValue, Object actualValue) {
		this.stringValue = stringValue;
		this.expectedValue = actualValue;
	}
	String stringValue;
	Object expectedValue;
}
