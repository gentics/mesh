package com.gentics.mesh.core;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.dagger.DaggerOrientDBMeshComponent;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshComponent.Builder;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.test.context.MeshInstanceProvider;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.context.ThrowingFunction;
import com.gentics.mesh.util.UUIDUtil;

/**
 * OrientDB-based Mesh instance provider.
 * 
 * @author plyhun
 *
 */
public class OrientDBMeshInstanceProvider implements MeshInstanceProvider<OrientDBMeshOptions> {
	
	private final MeshComponent.Builder componentBuilder;
	private final OrientDBMeshOptions meshOptions;
	
	public OrientDBMeshInstanceProvider(MeshOptions injectedMeshOptions) {
		componentBuilder = DaggerOrientDBMeshComponent.builder();
		meshOptions = (OrientDBMeshOptions) injectedMeshOptions;
	}

	@Override
	public void initStorage(MeshTestSetting settings) throws IOException {
		// The database provider will switch to in memory mode when no directory has been specified.
		GraphStorageOptions storageOptions = meshOptions.getStorageOptions();

		String graphPath = null;
		if (!settings.inMemoryDB() || settings.clusterMode()) {
			graphPath = "target/graphdb_" + UUIDUtil.randomUUID();
			File directory = new File(graphPath);
			directory.deleteOnExit();
			directory.mkdirs();
		}
		if (!settings.inMemoryDB() && settings.startStorageServer()) {
			storageOptions.setStartServer(true);
		}
		// Increase timeout to high load during testing
		storageOptions.setDirectory(graphPath);
		storageOptions.setSynchronizeWrites(true);
	}

	@Override
	public void initFolders(ThrowingFunction<String, String, IOException> pathProvider) throws IOException {
		String backupPath = pathProvider.apply("backups");
		meshOptions.getStorageOptions().setBackupDirectory(backupPath);

		String exportPath = pathProvider.apply("exports");
		meshOptions.getStorageOptions().setExportDirectory(exportPath);
	}

	@Override
	public void cleanupPhysicalStorage() throws IOException {
		String dir = meshOptions.getStorageOptions().getDirectory();
		File dbDir = new File(dir);
		FileUtils.deleteDirectory(dbDir);
	}

	@Override
	public Builder getComponentBuilder() {
		return componentBuilder;
	}
}
