package com.gentics.mesh.demo;

import java.io.File;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.util.UUIDUtil;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class DemoDumpConfiguration {

	@PostConstruct
	public void setup() {
		MeshFactoryImpl.clear();
		MeshOptions options = new MeshOptions();

		String uploads = "target/dump/testuploads_" + UUIDUtil.randomUUID();
		new File(uploads).mkdirs();
		options.getUploadOptions().setDirectory(uploads);

		String targetTmpDir = "target/dump/tmp_" + UUIDUtil.randomUUID();
		new File(targetTmpDir).mkdirs();
		options.getUploadOptions().setTempDirectory(targetTmpDir);

		String imageCacheDir = "target/dump/image_cache_" + UUIDUtil.randomUUID();
		new File(imageCacheDir).mkdirs();
		options.getImageOptions().setImageCacheDirectory(imageCacheDir);

		// The database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory("target/dump/graphdb");
		options.getSearchOptions().setDirectory("target/dump/searchindex");
		Mesh.mesh(options);
	}

}
