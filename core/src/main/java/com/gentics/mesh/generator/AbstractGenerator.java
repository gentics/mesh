package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public abstract class AbstractGenerator {

	protected File outputFolder;
	
	public AbstractGenerator() {
	}

	public AbstractGenerator(File outputFolder) throws IOException {
		this.outputFolder = outputFolder;
		cleanOutputFolder(outputFolder);
	}

	protected static File cleanOutputFolder(File folder) throws IOException {
		if (folder.exists()) {
			FileUtils.deleteDirectory(folder);
		}
		folder.mkdirs();
		return folder;
	}

}
