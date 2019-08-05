package com.gentics.mesh;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static com.gentics.mesh.MeshEnv.MESH_CONF_FILENAME;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gentics.mesh.cli.MeshCLI;
import com.gentics.mesh.cli.MeshNameProvider;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility class that is used to create and load mesh options.
 */
public final class OptionsLoader {

	private static final Logger log = LoggerFactory.getLogger(OptionsLoader.class);

	private OptionsLoader() {

	}

	/**
	 * Load the main mesh configuration file.
	 * 
	 * @param args
	 */
	public static MeshOptions createOrloadOptions(String... args) {
		MeshOptions options = loadMeshOptions();
		applyEnvironmentVariables(options);
		applyCommandLineArgs(options, args);
		options.validate();
		return options;
	}

	/**
	 * Check which environment variables have been set and override those in the provided options.
	 * 
	 * @param options
	 */
	private static void applyEnvironmentVariables(MeshOptions options) {
		options.overrideWithEnv();
	}

	/**
	 * Parse the command line arguments and update the mesh options accordingly.
	 * 
	 * @param options
	 * @param args
	 */
	private static void applyCommandLineArgs(MeshOptions options, String... args) {
		try {
			CommandLine commandLine = MeshCLI.parse(args);

			if (commandLine.hasOption(MeshCLI.HELP)) {
				MeshCLI.printHelp();
				System.exit(0);
			}

			String esUrl = commandLine.getOptionValue(MeshCLI.ELASTICSEARCH_URL);
			if (esUrl != null) {
				options.getSearchOptions().setUrl(esUrl);
			}

			String embeddedEsFlag = commandLine.getOptionValue(MeshCLI.EMBEDDED_ELASTICSEARCH);
			if (embeddedEsFlag != null) {
				options.getSearchOptions().setStartEmbedded(Boolean.valueOf(embeddedEsFlag));
			}

			boolean disableES = commandLine.hasOption(MeshCLI.DISABLE_ELASTICSEARCH);
			if (disableES) {
				options.getSearchOptions().setUrl(null);
				options.getSearchOptions().setStartEmbedded(false);
			}

			String adminPassword = commandLine.getOptionValue(MeshCLI.RESET_ADMIN_PASSWORD);
			options.setAdminPassword(adminPassword);

			if (commandLine.hasOption(MeshCLI.INIT_CLUSTER)) {
				options.setInitCluster(true);
			}

			// Check whether a custom node parameter has been set
			String cliNodeName = commandLine.getOptionValue(MeshCLI.NODE_NAME);
			if (!isEmpty(cliNodeName)) {
				options.setNodeName(cliNodeName);
			}

			// Check whether a custom cluster parameter has been set
			String cliClusterName = commandLine.getOptionValue(MeshCLI.CLUSTER_NAME);
			if (!isEmpty(cliClusterName)) {
				options.getClusterOptions().setClusterName(cliClusterName);
				options.getClusterOptions().setEnabled(true);
			}

			String httpPort = commandLine.getOptionValue(MeshCLI.HTTP_PORT);
			if (!isEmpty(httpPort)) {
				options.getHttpServerOptions().setPort(Integer.valueOf(httpPort));
			}
		} catch (ParseException e) {
			log.error("Error while parsing arguments {" + e.getMessage() + "}");
			if (log.isDebugEnabled()) {
				log.debug("Error while parsing argument", e);
			}
			throw new RuntimeException("Error while parsing arguments {" + e.getMessage() + "}");
		}
	}

	/**
	 * Try to load the mesh options from different locations (classpath, config folder). Otherwise a default configuration will be generated.
	 * 
	 * @return
	 */
	private static MeshOptions loadMeshOptions() {

		File confFile = new File(CONFIG_FOLDERNAME, MESH_CONF_FILENAME);
		MeshOptions options = null;
		InputStream ins = Mesh.class.getResourceAsStream("/" + MESH_CONF_FILENAME);
		// 1. Try to load from classpath
		if (ins != null) {
			log.info("Loading configuration file from classpath.");
			options = loadConfiguration(ins);
			if (options != null) {
				return options;
			} else {
				throw new RuntimeException("Could not read configuration file");
			}
		} else {
			log.info("Configuration file {" + MESH_CONF_FILENAME + "} was not found within classpath.");
		}
		// 2. Try to use config file
		if (confFile.exists()) {
			try {
				log.info("Loading configuration file {" + confFile + "}.");
				MeshOptions configuration = loadConfiguration(new FileInputStream(confFile));
				if (configuration != null) {
					return configuration;
				}
			} catch (FileNotFoundException e) {
				log.error("Could not load configuration file {" + confFile.getAbsolutePath() + "}.", e);
			}
		} else {
			log.info("Configuration file {" + CONFIG_FOLDERNAME + "/" + MESH_CONF_FILENAME + "} was not found within filesystem.");

			ObjectMapper mapper = getYAMLMapper();
			try {
				// Generate default config
				options = generateDefaultConfig();
				FileUtils.writeStringToFile(confFile, mapper.writeValueAsString(options), StandardCharsets.UTF_8, false);
				log.info("Saved default configuration to file {" + confFile.getAbsolutePath() + "}.");
			} catch (IOException e) {
				log.error("Error while saving default configuration to file {" + confFile.getAbsolutePath() + "}.", e);
			}
		}
		// 2. No luck - use default config
		log.info("Loading default configuration.");
		return options;

	}

	/**
	 * Generate a default configuration with meaningful default settings. The keystore password will be randomly generated and set.
	 * 
	 * @return
	 */
	public static MeshOptions generateDefaultConfig() {
		MeshOptions options = new MeshOptions();
		options.getAuthenticationOptions().setKeystorePassword(UUIDUtil.randomUUID());
		options.setNodeName(MeshNameProvider.getInstance().getRandomName());
		return options;
	}

	/**
	 * Return the preconfigured object mapper which is used to transform YAML documents.
	 * 
	 * @return
	 */
	public static ObjectMapper getYAMLMapper() {
		YAMLFactory factory = new YAMLFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		return mapper;
	}

	/**
	 * Load the configuration from the stream.
	 * 
	 * @param ins
	 * @return
	 */
	private static MeshOptions loadConfiguration(InputStream ins) {
		if (ins == null) {
			log.info("Config file {" + MESH_CONF_FILENAME + "} not found. Using default configuration.");
			return new MeshOptions();
		}

		ObjectMapper mapper = getYAMLMapper();
		try {
			return mapper.readValue(ins, MeshOptions.class);
		} catch (Exception e) {
			log.error("Could not parse configuration.", e);
			throw new RuntimeException("Could not parse options file", e);
		}
	}

}
