package com.gentics.mesh.generator;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.OptionsLoader;

public class RAMLGeneratorRunner {

	private static File outputFolder = new File("target", "api");

	public static void main(String[] args) throws Exception {
		if (outputFolder.exists()) {
			FileUtils.deleteDirectory(outputFolder);
		}
		File conf = new File(OptionsLoader.MESH_CONF_FILENAME);
		if (conf.exists()) {
			conf.delete();
		}

		RAMLGenerator generator = new RAMLGenerator(outputFolder);
		String raml = generator.generate();
		generator.writeFile("api.raml", raml);
	}
}
