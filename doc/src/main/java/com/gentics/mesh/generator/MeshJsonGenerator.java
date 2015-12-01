package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.etc.config.MeshOptions;

public class MeshJsonGenerator {

	public static void main(String[] args) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		MeshOptions conf = new MeshOptions();
		conf.setTempDirectory("/opt/mesh/tmp");
		conf.getUploadOptions().setTempDirectory("/opt/mesh/tmp/temp-uploads");
		String baseDirProp = System.getProperty("baseDir");
		if (baseDirProp == null) {
			baseDirProp = "target" + File.separator + "mesh-json";
		}
		new File(baseDirProp).mkdirs();
		File outputFile = new File(baseDirProp, "mesh-config.json");

		FileUtils.writeStringToFile(outputFile, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(conf));
	}

}
