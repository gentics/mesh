package com.gentics.mesh.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.gentics.vertx.openapi.OpenAPIv3Generator;
import com.gentics.vertx.openapi.model.ExtendedSecurityScheme;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.InParameter;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.vertx.ext.web.Router;

/**
 * Overridden OpenAPI v3 generator, adding Mesh specific features.
 */
public class MeshOpenAPIv3Generator extends OpenAPIv3Generator {

	protected final ExtendedSecurityScheme securityBearerAuth;

	public MeshOpenAPIv3Generator(String version, List<String> servers,
			Optional<? extends Collection<Pattern>> maybePathBlacklist,
			Optional<? extends Collection<Pattern>> maybePathWhitelist) {	
		this(version, servers, false, maybePathBlacklist, maybePathWhitelist);
	}

	public MeshOpenAPIv3Generator(String version, List<String> servers,
			boolean secureByDefault,
			Optional<? extends Collection<Pattern>> maybePathBlacklist,
			Optional<? extends Collection<Pattern>> maybePathWhitelist) {	
		super(version, servers, Collections.singletonMap("bearerAuth", new ExtendedSecurityScheme(secureByDefault)), maybePathBlacklist, maybePathWhitelist);
		securityBearerAuth = security.get("bearerAuth");
		securityBearerAuth.getScheme().setScheme("bearer");
		securityBearerAuth.getScheme().setType(SecurityScheme.Type.HTTP);
		securityBearerAuth.getScheme().setBearerFormat("JWT");
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
		return generate("Gentics Mesh REST API", routers, format, pretty, useVersion31);
	}

	/**
	 * Generate the specification using some functional shortcuts and an API name.
	 * 
	 * @param routers
	 * @param format
	 * @param pretty
	 * @param useVersion31
	 * @return
	 * @throws OpenAPIGenerationException
	 */
	public String generate(String name, Map<Router, String> routers, Format format, boolean pretty, boolean useVersion31) throws OpenAPIGenerationException {
		return generate(name, routers, format, pretty, useVersion31, 
				// transform project path item
				Optional.of((path, item) -> {
					if (path.contains("/{project}/")) {
						Parameter projectNameParam = new Parameter().name("project").in(InParameter.PATH.toString()).schema(new Schema<String>().type("string").description("Name of the related project"));
						item.readOperations().stream()
							.forEach(o -> o.getParameters().stream().filter(p -> "project".equals(p.getName())).findAny()
									.ifPresentOrElse(present -> {
										// already exists, no action
									}, () -> o.addParametersItem(projectNameParam)));
					}
					return path;
				}), 
				// fill the component model
				Optional.empty());
	}
}
