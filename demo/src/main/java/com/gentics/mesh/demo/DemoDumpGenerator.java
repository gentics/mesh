package com.gentics.mesh.demo;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.search.SearchProvider;

/**
 * The demo dump generator is used to create a mesh database dump which contains the demo data. This dump is packaged and later placed within the final mesh jar
 * in order to accelerate demo startup.
 */
public class DemoDumpGenerator {

	public static void main(String[] args) throws Exception {
		FileUtils.deleteDirectory(new File("target/dump"));
		initPaths();
		new DemoDumpGenerator().dump();
	}

	public static void initPaths() {
		MeshFactoryImpl.clear();
		MeshOptions options = new MeshOptions();

		// Prefix all default directories in order to place them into the dump directory
		String uploads = "target/dump/" + options.getUploadOptions().getDirectory();
		new File(uploads).mkdirs();
		options.getUploadOptions().setDirectory(uploads);

		String targetTmpDir = "target/dump/" + options.getUploadOptions().getTempDirectory();
		new File(targetTmpDir).mkdirs();
		options.getUploadOptions().setTempDirectory(targetTmpDir);

		String imageCacheDir = "target/dump/" + options.getImageOptions().getImageCacheDirectory();
		new File(imageCacheDir).mkdirs();
		options.getImageOptions().setImageCacheDirectory(imageCacheDir);

		// The database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory("target/dump/" + options.getStorageOptions().getDirectory());
		options.getSearchOptions().setDirectory("target/dump/" + options.getSearchOptions().getDirectory());
		Mesh.mesh(options);
	}

	/**
	 * Invoke the demo data dump.
	 * 
	 * @throws Exception
	 */
	private void dump() throws Exception {
		// Cleanup in preparation for dumping the demo data
		cleanup();
		MeshComponent meshDagger = MeshInternal.create();

		// Initialise mesh
		BootstrapInitializer boot = meshDagger.boot();
		DemoDataProvider provider = new DemoDataProvider(meshDagger.database(), meshDagger.meshLocalClientImpl());
		SearchProvider searchProvider = meshDagger.searchProvider();
		invokeDump(boot, provider);
		searchProvider.getNode().close();
		System.exit(0);
	}

	/**
	 * Cleanup the dump directories and remove the existing mesh config.
	 * 
	 * @throws IOException
	 */
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(new File("target" + File.separator + "dump"));
		File confFile = new File("mesh.json");
		if (confFile.exists()) {
			confFile.delete();
		}
	}

	/**
	 * Invoke the demo database setup.
	 * 
	 * @param boot
	 * @param provider
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws MeshSchemaException
	 * @throws InterruptedException
	 */
	public void invokeDump(BootstrapInitializer boot, DemoDataProvider provider)
			throws JsonParseException, JsonMappingException, IOException, MeshSchemaException, InterruptedException {
		boot.initSearchIndexHandlers();
		boot.initMandatoryData();
		boot.initPermissions();
		boot.markChangelogApplied();
		boot.createSearchIndicesAndMappings();

		// Setup demo data
		provider.setup();
	}

}
