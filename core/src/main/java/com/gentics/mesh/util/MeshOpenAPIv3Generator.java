package com.gentics.mesh.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.reflections.Reflections;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.vertx.openapi.OpenAPIv3Generator;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.vertx.ext.web.Router;

/**
 * Overridden OpenAPI v3 generator, adding Mesh specific features.
 */
public class MeshOpenAPIv3Generator extends OpenAPIv3Generator {

	public MeshOpenAPIv3Generator(String version, List<String> servers,
			Optional<? extends Collection<Pattern>> maybePathBlacklist,
			Optional<? extends Collection<Pattern>> maybePathWhitelist) {
		super(version, servers, maybePathBlacklist, maybePathWhitelist);
	}

	@Override
	protected void addSecurity(OpenAPI openApi) {
		Components components;
		if (openApi.getComponents() == null) {
			components = new Components();
			openApi.setComponents(components);
		} else {
			components = openApi.getComponents();
		}
		SecurityScheme securityBearerAuth = new SecurityScheme();
		securityBearerAuth.setScheme("bearer");
		securityBearerAuth.setType(SecurityScheme.Type.HTTP);
		securityBearerAuth.setBearerFormat("JWT");
		components.addSecuritySchemes("bearerAuth", securityBearerAuth);
		//TODO OAuth2
		SecurityRequirement reqBearerAuth = new SecurityRequirement();
		reqBearerAuth.addList("bearerAuth");
		openApi.addSecurityItem(reqBearerAuth);
	}

	/**
	 * Generate the specification using some functional shortcuts.
	 * 
	 * @param routers
	 * @param format
	 * @param pretty
	 * @param useVersion31
	 * @return
	 * @throws OpenAPIGenerationException
	 */
	public String generate(Map<Router, String> routers, Format format, boolean pretty, boolean useVersion31) throws OpenAPIGenerationException {
		return generate(routers, format, pretty, useVersion31, 
				// transform project path item
				Optional.of((path, item) -> {
					if (path.contains("/{project}/")) {
						Parameter projectNameParam = new Parameter().name("project").in(InParameter.PATH.toString()).schema(new Schema<String>().type("string").description("Uuid of the related project"));
						item.readOperations().stream()
							.forEach(o -> o.getParameters().stream().filter(p -> "project".equals(p.getName())).findAny()
									.ifPresentOrElse(present -> {
										// already exists, no action
									}, () -> o.addParametersItem(projectNameParam)));
					}
					return path;
				}), 
				// fill the component model
				Optional.of(() -> Collections.unmodifiableCollection(
						new Reflections("com.gentics.mesh")
						.getSubTypesOf(RestModel.class)
						.stream()
						.filter(ty -> ty.getPackageName().startsWith("com.gentics.mesh"))
						.collect(Collectors.toSet()))));
	}
}
