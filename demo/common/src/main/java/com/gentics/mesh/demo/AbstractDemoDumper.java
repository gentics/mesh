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
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.UUIDUtil;

/**
 * The demo dumper is used to create a mesh database dump which contains the demo data. This dump is packaged and later placed within the final mesh jar
 * in order to accelerate demo startup.
 */
public abstract class AbstractDemoDumper<T extends MeshOptions> extends AbstractMeshOptionsDemoContext<T> implements DemoDumper {

	private MeshComponent meshInternal;

	public AbstractDemoDumper(String[] args, Class<T> optionsClass) {
		super(args, optionsClass);
	}

	/**
	 * Run the demo dump generator which will write the dump to the filesystem.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public void dump(String[] args) throws Exception {
		try {
			cleanup();
			init();
			dump();
			shutdown();
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public MeshComponent getMeshInternal() {
		return meshInternal;
	}

	@Override
	public void init() throws Exception {
		T options = getOptions();

		options.getSearchOptions().setUrl(null);
		options.setInitialAdminPassword("admin");
		options.setForceInitialAdminPasswordReset(false);

		setupOptions(options);

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

		// Setup the java keystore
		options.getAuthenticationOptions().setKeystorePassword(UUIDUtil.randomUUID());
		File keyStoreFile = new File("target", "keystore_" + UUIDUtil.randomUUID() + ".jks");
		options.getAuthenticationOptions().setKeystorePath(keyStoreFile.getAbsolutePath());
		String keyStorePass = options.getAuthenticationOptions().getKeystorePassword();
		if (!keyStoreFile.exists()) {
			KeyStoreHelper.gen(keyStoreFile.getAbsolutePath(), keyStorePass);
		}
		options.setNodeName("dumpGenerator");
		init(options);
	}

	/**
	 * Initialize mesh and prepare the demo generator.
	 * 
	 * @param options
	 * @throws Exception
	 */
	public void init(T options) throws Exception {
		Mesh mesh = Mesh.create(options);
		mesh.run(false);
		meshInternal = mesh.internal();
	}

	@Override
	public void dump() throws Exception {
		// Initialise demo data
		BootstrapInitializer boot = meshInternal.boot();
		DemoDataProvider provider = new DemoDataProvider(meshInternal.database(), meshInternal.meshLocalClientImpl());
		invokeDump(boot, provider);
	}

	/**
	 * Shutdown the mesh server.
	 * 
	 * @throws Exception
	 */
	protected void shutdown() throws Exception {
		meshInternal.boot().mesh().shutdown();
		Thread.sleep(2000);
		System.exit(0);
	}

	@Override
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

		// Setup demo data
		provider.setup();
	}
}
