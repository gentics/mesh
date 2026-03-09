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

	protected static ExtendedSecurityScheme securityBearerAuth;

	static {
		securityBearerAuth = new ExtendedSecurityScheme(false);
		securityBearerAuth.getScheme().setScheme("bearer");
		securityBearerAuth.getScheme().setType(SecurityScheme.Type.HTTP);
		securityBearerAuth.getScheme().setBearerFormat("JWT");
	}

	public MeshOpenAPIv3Generator(String version, List<String> servers,
			Optional<? extends Collection<Pattern>> maybePathBlacklist,
			Optional<? extends Collection<Pattern>> maybePathWhitelist) {	
		super(version, servers, Collections.singletonMap("bearerAuth", securityBearerAuth), maybePathBlacklist, maybePathWhitelist);
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
				Optional.of(() -> Collections.unmodifiableCollection(
						new Reflections("com.gentics.mesh")
						.getSubTypesOf(RestModel.class)
						.stream()
						.filter(ty -> ty.getPackageName().startsWith("com.gentics.mesh"))
						.collect(Collectors.toSet()))));
	}

	@Override
	protected String getComponentName(Class<?> cls) {
		String componentName = super.getComponentName(cls);

		if (componentName.endsWith("Impl")) {
			componentName = componentName.substring(0, componentName.length()-4);
		}
		return componentName;
	}
}
