package com.gentics.mesh.plugin.forms.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.plugin.common.MeshPluginException;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.digester.plugins.PluginException;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Options for handling potentially unsafe posted form data.
 */
public class SecurityOptions {

	private static final String INJECTION_PREVENTION_ENV = "MESH_PLUGIN_FORMS_INJECTION_PREVENTION";

	private static final InjectionPreventionMode DEFAULT_INJECTION_PREVENTION = InjectionPreventionMode.NONE;
	private static final Map<String, String> DEFAULT_SANITATION_RULES = new HashMap<>();

	@JsonProperty(defaultValue = "NONE")
	@JsonPropertyDescription("Set the mode for handling injection prevention.")
	@EnvironmentVariable(name = INJECTION_PREVENTION_ENV, description = "Override the injection prevention mode (default: NONE).")
	private InjectionPreventionMode injectionPreventionMode = DEFAULT_INJECTION_PREVENTION;

	private Map<String, String> sanitationRules = DEFAULT_SANITATION_RULES;
	private Map<Pattern, String> compiledSanitationRules;
	private boolean sanitationRulesChanged = true;

	@Nonnull
	public InjectionPreventionMode getInjectionPreventionMode() {
		if (injectionPreventionMode == null) {
			injectionPreventionMode = DEFAULT_INJECTION_PREVENTION;
		}

		return injectionPreventionMode;
	}

	public SecurityOptions setInjectionPreventionMode(InjectionPreventionMode injectionPreventionMode) {
		this.injectionPreventionMode = injectionPreventionMode;

		return this;
	}

	public Map<String, String> getSanitationRules() {
		if (sanitationRules == null) {
			sanitationRules = DEFAULT_SANITATION_RULES;
		}

		return sanitationRules;
	}

	public SecurityOptions setSanitationRules(Map<String, String> sanitationRules) {
		this.sanitationRules = sanitationRules;
		this.sanitationRulesChanged = true;

		return this;
	}

	/**
	 * Get the sanitation rules map where the keys are already compiled
	 * to {@link Pattern Patterns}.
	 *
	 * @return The sanitation rules where the keys are already compiled to
	 * 		{@code Patterns}.
	 */
	@JsonIgnore
	public Map<Pattern, String> getCompiledSanitationRules() {
		if (compiledSanitationRules == null || sanitationRulesChanged) {
			compiledSanitationRules = sanitationRules.entrySet().stream()
				.map(e -> Pair.of(Pattern.compile(e.getKey()), e.getValue()))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue));

			sanitationRulesChanged = false;
		}

		return compiledSanitationRules;
	}

	/**
	 * Verify that the sanitation rule keys are all valid strings for
	 * {@link Pattern#compile(String)}.
	 *
	 * @throws MeshPluginException When one of the sanitation rule keys
	 * 		is not a valid regular expression.
	 */
	@JsonIgnore
	public void validateSanitationRules() throws MeshPluginException {
		try {
			getCompiledSanitationRules();
		} catch (Exception e) {
			throw new MeshPluginException(
				"Could not initialize sanitation rules: " + e.getMessage(),
				e,
				HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
		}
	}
}
