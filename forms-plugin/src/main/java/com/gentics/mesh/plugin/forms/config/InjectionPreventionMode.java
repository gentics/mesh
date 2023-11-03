package com.gentics.mesh.plugin.forms.config;

/**
 * Modes for how to handle possible code or URL injections.
 */
public enum InjectionPreventionMode {
	/** The plugin will not check or sanitize any input, but take it as is. */
	NONE,
	/** The plugin will validate the input and throw a validation error when the input contains any of the patterns in the sanitation rules. */
	REJECT,
	/** The plugin will validate the input and throw a validation error when the input contains any of the patterns in the sanitation rules or any markup. */
	REJECT_MARKUP,
	/** The plugin will sanitize all incoming input according to configured sanitation rules. */
	SANITIZE,
	/** The plugin will sanitize all incoming input according to configured sanitation rules and additionally all markup will be removed. */
	SANITIZE_MARKUP
}
