package com.gentics.mesh.generator;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * Abstract implementation for an example generator.
 */
public abstract class AbstractGenerator {

	protected File outputFolder;

	public AbstractGenerator() {
	}

	public AbstractGenerator(File outputFolder, boolean cleanup) throws IOException {
		this.outputFolder = outputFolder;
		if (cleanup) {
			cleanOutputFolder(outputFolder);
		}
	}

	public AbstractGenerator(File outputFolder) throws IOException {
		this(outputFolder, true);
	}

	protected static File cleanOutputFolder(File folder) throws IOException {
		if (folder.exists()) {
			FileUtils.deleteDirectory(folder);
		}
		folder.mkdirs();
		return folder;
	}

}
