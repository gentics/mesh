package com.gentics.mesh.generator;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static com.gentics.mesh.MeshEnv.MESH_CONF_FILENAME;

import java.io.File;
import java.io.IOException;

/**
 * Runner for various example generators.
 */
public class ExampleGeneratorRunner {

	public static File OUTPUT_ROOT_FOLDER = new File("src/main/docs/examples");

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

		// Generate RAML
		RAMLGenerator generator = new RAMLGenerator(OUTPUT_ROOT_FOLDER, "api.raml", null, false);
		generator.run();

		// Generate the RAML for the raml2html docs. This raml includes a markdown table
		generator = new RAMLGenerator(OUTPUT_ROOT_FOLDER, "api-docs.raml", (mimeType, clazz) -> {
			try {
				mimeType.setSchema(tableGen.renderModelTableViaSchema(clazz, tableGen.getTemplate("model-props-markdown-table.hbs")));
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Could not render table");
			}
		}, true);
		generator.run();

		// Generate elasticsearch flattened models
		SearchModelGenerator searchModelGen = new SearchModelGenerator(OUTPUT_ROOT_FOLDER);
		searchModelGen.run();
	}

	private static void cleanConf() {
		File conf = new File(CONFIG_FOLDERNAME + "/" + MESH_CONF_FILENAME);
		if (conf.exists()) {
			conf.delete();
		}
	}

}
