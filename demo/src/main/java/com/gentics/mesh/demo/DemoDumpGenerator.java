package com.gentics.mesh.demo;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.node.Node;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.UUIDUtil;

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
		// 1. Cleanup in preparation for dumping the demo data
		cleanup();

		// 2. Setup dagger
		MeshComponent meshDagger = MeshInternal.create();

		// 3. Setup GraphDB
		new DatabaseHelper(meshDagger.database()).init();

		MeshOptions options = Mesh.mesh().getOptions();
		options.getAuthenticationOptions().setKeystorePassword(UUIDUtil.randomUUID());
		String keyStorePath = options.getAuthenticationOptions().getKeystorePath();
		String keyStorePass = options.getAuthenticationOptions().getKeystorePassword();
		if (!new File(keyStorePath).exists()) {
			KeyStoreHelper.gen(keyStorePath, keyStorePass);
		}

		// 4. Initialise mesh
		BootstrapInitializer boot = meshDagger.boot();
		boot.initMandatoryData();
		boot.initPermissions();
		boot.markChangelogApplied();
		boot.createSearchIndicesAndMappings();

		// 5. Init demo data
		DemoDataProvider provider = new DemoDataProvider(meshDagger.database(), meshDagger.meshLocalClientImpl());
		SearchProvider searchProvider = meshDagger.searchProvider();
		invokeDump(boot, provider);

		// Close the elastic search instance
		org.elasticsearch.node.Node esNode = null;
		if (searchProvider.getNode() instanceof org.elasticsearch.node.Node) {
			esNode = (Node) searchProvider.getNode();
		} else {
			throw new MeshConfigurationException("Unable to get elasticsearch instance from search provider got {" + searchProvider.getNode() + "}");
		}
		esNode.close();

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
		boot.initMandatoryData();
		boot.initPermissions();
		boot.markChangelogApplied();
		boot.createSearchIndicesAndMappings();

		// Setup demo data
		provider.setup();
	}

}
