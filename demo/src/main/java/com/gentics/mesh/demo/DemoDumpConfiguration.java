package com.gentics.mesh.demo;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;

import dagger.Module;

/**
 * Spring configuration which is used in combination with the {@link DemoDumpGenerator}.
 */
@Module
public class DemoDumpConfiguration {

	public DemoDumpConfiguration() {
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
		options.getSearchOptions().setHttpEnabled(true);
		Mesh.mesh(options);
	}

}
