package com.gentics.mesh.generator;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static com.gentics.mesh.MeshEnv.MESH_CONF_FILENAME;

import java.io.File;

public class ExampleGeneratorRunner {

	public static File OUTPUT_ROOT_FOLDER = new File("src/main/docs/examples");

	public static void main(String[] args) throws Exception {
		if (!OUTPUT_ROOT_FOLDER.exists()) {
			OUTPUT_ROOT_FOLDER.mkdirs();
		}
		cleanConf();

		TableGenerator tableGen = new TableGenerator(OUTPUT_ROOT_FOLDER);
		tableGen.run();

		ModelExampleGenerator restModelGen = new ModelExampleGenerator(OUTPUT_ROOT_FOLDER);
		restModelGen.run();

		RAMLGenerator generator = new RAMLGenerator(OUTPUT_ROOT_FOLDER);
		generator.run();

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
