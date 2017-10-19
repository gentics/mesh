package com.gentics.mesh.demo;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
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
		DemoDumpGenerator generator = new DemoDumpGenerator();
		generator.cleanup();
		generator.init();
		generator.dump();
		generator.shutdown();
	}

	public void init() throws Exception {
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

		// 2. Setup the java keystore
		options.getAuthenticationOptions().setKeystorePassword(UUIDUtil.randomUUID());
		File keyStoreFile = new File("target", "keystore_" + UUIDUtil.randomUUID() + ".jks");
		options.getAuthenticationOptions().setKeystorePath(keyStoreFile.getAbsolutePath());
		String keyStorePass = options.getAuthenticationOptions().getKeystorePassword();
		if (!keyStoreFile.exists()) {
			KeyStoreHelper.gen(keyStoreFile.getAbsolutePath(), keyStorePass);
		}
		options.setNodeName("dumpGenerator");
		Mesh mesh = Mesh.mesh(options);

		// 1. Setup dagger
		MeshComponent meshDagger = MeshInternal.create();
		BootstrapInitializer boot = meshDagger.boot();
		boot.init(mesh, false, options, null);

	}

	/**
	 * Invoke the demo data dump.
	 * 
	 * @throws Exception
	 */
	public void dump() throws Exception {

		// 4. Initialise demo data
		MeshComponent meshDagger = MeshInternal.get();
		BootstrapInitializer boot = meshDagger.boot();
		DemoDataProvider provider = new DemoDataProvider(meshDagger.database(), meshDagger.meshLocalClientImpl(), boot);
		invokeDump(boot, provider);

	}

	private void shutdown() throws MeshConfigurationException, InterruptedException {
		// Close the elastic search instance
		SearchProvider searchProvider = MeshInternal.get().searchProvider();
		if (searchProvider.getClient() !=null) {
			searchProvider.refreshIndex("_all");
		} else {
			throw new MeshConfigurationException("Unable to get elasticsearch instance from search provider got {" + searchProvider.getClient() + "}");
		}
		searchProvider.stop();
		Thread.sleep(5000);
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
	private void invokeDump(BootstrapInitializer boot, DemoDataProvider provider)
			throws JsonParseException, JsonMappingException, IOException, MeshSchemaException, InterruptedException {
		boot.initMandatoryData();
		boot.initOptionalData(true);
		boot.initPermissions();
		boot.markChangelogApplied();
		boot.createSearchIndicesAndMappings();

		// Setup demo data
		provider.setup();
	}

}
