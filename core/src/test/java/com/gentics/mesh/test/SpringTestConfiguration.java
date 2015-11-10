package com.gentics.mesh.test;

import java.io.File;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.util.UUIDUtil;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class SpringTestConfiguration {

	public static boolean ignored = false;

	@Bean
	public DummySearchProvider dummySearchProvider() {
		return new DummySearchProvider();
	}

	@Bean
	public SearchProvider searchProvider() {
		// For testing it is not needed to start ES in most cases. This will speedup test execution since ES does not need to initialize.
		return dummySearchProvider();
	}

	@PostConstruct
	public void setup() {
		if (ignored) {
			return;
		}
		MeshFactoryImpl.clear();
		MeshOptions options = new MeshOptions();

		String uploads = "target/testuploads_" + UUIDUtil.randomUUID();
		String targetTmpDir = "target/tmp_" + UUIDUtil.randomUUID();
		new File(uploads).mkdirs();
		new File(targetTmpDir).mkdirs();
		options.getUploadOptions().setDirectory(uploads);
		options.getUploadOptions().setTempDirectory(targetTmpDir);
		options.getHttpServerOptions().setPort(TestUtil.getRandomPort());
		// The database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);
		options.getAuthenticationOptions().setSignatureSecret("secret");
		options.getAuthenticationOptions().setKeystorePath("keystore.jceks");
		Mesh.mesh(options);
	}

}
