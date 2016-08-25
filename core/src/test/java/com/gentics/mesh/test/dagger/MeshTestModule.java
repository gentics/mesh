package com.gentics.mesh.test.dagger;

import java.io.File;

import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.MeshModule;
import com.gentics.mesh.etc.config.AuthenticationOptions.AuthenticationMethod;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.test.performance.TestUtils;
import com.gentics.mesh.util.UUIDUtil;

import dagger.Module;
import dagger.Provides;

@Singleton
@Module
public class MeshTestModule extends MeshModule {

	@Provides
	public DummySearchProvider dummySearchProvider() {
		return new DummySearchProvider();
	}

	public static void init() {
		MeshFactoryImpl.clear();
		MeshOptions options = new MeshOptions();

		String uploads = "target/testuploads_" + UUIDUtil.randomUUID();
		new File(uploads).mkdirs();
		options.getUploadOptions().setDirectory(uploads);

		String targetTmpDir = "target/tmp_" + UUIDUtil.randomUUID();
		new File(targetTmpDir).mkdirs();
		options.getUploadOptions().setTempDirectory(targetTmpDir);

		String imageCacheDir = "target/image_cache_" + UUIDUtil.randomUUID();
		new File(imageCacheDir).mkdirs();
		options.getImageOptions().setImageCacheDirectory(imageCacheDir);

		options.getHttpServerOptions().setPort(TestUtils.getRandomPort());
		// The database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);
		options.getAuthenticationOptions().setAuthenticationMethod(AuthenticationMethod.JWT);
		options.getAuthenticationOptions().getJwtAuthenticationOptions().setSignatureSecret("secret");
		options.getAuthenticationOptions().getJwtAuthenticationOptions().setKeystorePath("keystore.jceks");
		Mesh.mesh(options);
	}

}
