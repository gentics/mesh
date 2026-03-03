package com.gentics.mesh.generator;

import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.gentics.mesh.util.MeshOpenAPIv3Generator;
import com.gentics.vertx.openapi.OpenAPIv3Generator;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;

import io.vertx.ext.web.Router;

/**
 * OpenAPI v3 API definition generator. Outputs JSON and YAML schemas.
 * 
 * @author plyhun
 *
 */
public class OpenAPIMockAPIGenerator extends AbstractEndpointGenerator<OpenAPIv3Generator> {

	private static final Logger log = LoggerFactory.getLogger(OpenAPIMockAPIGenerator.class);

	private final MeshOpenAPIv3Generator generator;
	private final Map<Router, String> routers = new HashMap<>();
	private final String fileName;

	public OpenAPIMockAPIGenerator() {
		super();
		this.fileName = null;
		this.generator = new MeshOpenAPIv3Generator(MeshVersion.getPlainVersion(), Collections.emptyList(),
				Optional.empty(), Optional.empty());
	}

	public OpenAPIMockAPIGenerator(File outputFolder, String fileName, boolean cleanup) throws IOException {
		super(new File(outputFolder, "api"), false);
		this.fileName = fileName;
		this.generator = new MeshOpenAPIv3Generator(MeshVersion.getPlainVersion(), Collections.emptyList(),
				Optional.empty(), Optional.empty());
	}

	public OpenAPIMockAPIGenerator(File outputFolder, String fileName) throws IOException {
		super(new File(outputFolder, "api"), false);
		this.fileName = fileName;
		this.generator = new MeshOpenAPIv3Generator(MeshVersion.getPlainVersion(), Collections.emptyList(),
				Optional.empty(), Optional.empty());
	}

	public String generate(String format) throws IOException, OpenAPIGenerationException {
		log.info("Starting OpenAPIv3 generation...");
		addCoreEndpoints(generator);
		addProjectEndpoints(generator);
		addExtraEndpoints(generator);

		return generator.generate(routers, Format.parse(format), true, false);
	}

	@Override
	protected void addEndpoints(String basePath, OpenAPIv3Generator consumer, AbstractInternalEndpoint verticle,
			boolean isProject) throws IOException {
		String fullPath = "/api/v" + CURRENT_API_VERSION + basePath + "/" + verticle.getBasePath();
		if (isEmpty(verticle.getBasePath())) {
			fullPath = "/api/v" + CURRENT_API_VERSION + basePath;
		}
		routers.put(verticle.getRouter(), fullPath);
	}

	/**
	 * Start the generator.
	 * 
	 * @throws IOException
	 * @throws OpenAPIGenerationException 
	 */
	public void run() throws IOException, OpenAPIGenerationException {
		String yaml = generate("yaml");
		writeFile(fileName, yaml);
	}
}
