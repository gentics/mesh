package com.gentics.mesh.demo;

import java.io.File;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;

/**
 * Spring configuration which is used in combination with the {@link DemoDumpGenerator}.
 */
@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class DemoDumpConfiguration {

	/**
	 * Flag to enable or disable this configuration.
	 */
	public static boolean enabled = false;

	@PostConstruct
	public void setup() {
		if (enabled) {
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
	}

}
