package com.gentics.mesh.plugin.forms.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;
import org.apache.commons.lang3.StringUtils;

public class PollOptions implements Option {

	public static final String RESULT_TEMPLATE_ENV = "MESH_PLUGIN_FORMS_POLL_RESULT_TEMPLATE";

	private static final String DEFAULT_RESULTS_TEMPLATE = "poll-results";

	@JsonProperty(defaultValue = DEFAULT_RESULTS_TEMPLATE)
	@JsonPropertyDescription("The name of the poll results template (without .hbs extension)")
	@EnvironmentVariable(name = RESULT_TEMPLATE_ENV, description = "Override the poll result template name")
	private String resultTemplate = DEFAULT_RESULTS_TEMPLATE;

	public String getResultTemplate() {
		return resultTemplate;
	}

	public PollOptions setResultTemplate(String resultTemplate) {
		this.resultTemplate = StringUtils.removeEnd(resultTemplate, ".hbs");

		return this;
	}
}
