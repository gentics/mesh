package com.gentics.mesh.generator;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static com.gentics.mesh.MeshEnv.MESH_CONF_FILENAME;

import java.io.File;

import com.gentics.mesh.generator.runner.OpenAPISpecGeneratorRunner;

/**
 * Runner for various example generators.
 */
public class ExampleGeneratorRunner {

	public static File DOCS_FOLDER = new File("src/main/docs");
	public static File OUTPUT_ROOT_FOLDER = new File(DOCS_FOLDER, "generated");

	/**
	 * Start the example generators.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (!OUTPUT_ROOT_FOLDER.exists()) {
			OUTPUT_ROOT_FOLDER.mkdirs();
		}
		cleanConf();

		// Generate asciidoc tables to be included in the docs.
		TableGenerator tableGen = new TableGenerator(OUTPUT_ROOT_FOLDER);
		tableGen.run();

		// Generate model examples (json files)
		ModelExampleGenerator restModelGen = new ModelExampleGenerator(OUTPUT_ROOT_FOLDER);
		restModelGen.run();

		// Generate OpenAPI base spec
		OpenAPISpecGeneratorRunner.main(new String[] { OUTPUT_ROOT_FOLDER.getPath() });

		// Generate the HTML API documentation and the request/response example files from the OpenAPI spec
		OpenAPIHtmlGenerator apiHtmlGenerator = new OpenAPIHtmlGenerator(OUTPUT_ROOT_FOLDER);
		apiHtmlGenerator.run();

		// Generate elasticsearch flattened models
		SearchModelGenerator searchModelGen = new SearchModelGenerator(OUTPUT_ROOT_FOLDER);
		searchModelGen.run();

		// Generate environment variable table
		EnvHelpGenerator envGen = new EnvHelpGenerator(OUTPUT_ROOT_FOLDER);
		envGen.run();

		// Generate CLI info
		CLIHelpGenerator cliGenerator = new CLIHelpGenerator(OUTPUT_ROOT_FOLDER);
		cliGenerator.run();

		// Event tables
		EventTableGenerator eventTable = new EventTableGenerator(OUTPUT_ROOT_FOLDER);
		eventTable.run();
	}

	private static void cleanConf() {
		File conf = new File(CONFIG_FOLDERNAME + "/" + MESH_CONF_FILENAME);
		if (conf.exists()) {
			conf.delete();
		}
	}
}
