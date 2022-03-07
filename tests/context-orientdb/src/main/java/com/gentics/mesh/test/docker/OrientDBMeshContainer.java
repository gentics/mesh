package com.gentics.mesh.test.docker;

import java.util.function.Function;

import org.testcontainers.images.builder.ImageFromDockerfile;

import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

public class OrientDBMeshContainer extends MeshContainer {

	public OrientDBMeshContainer(String imageName) {
		super(imageName);
	}

	public OrientDBMeshContainer(Function<MeshOptions, ImageFromDockerfile> imageProvider) {
		super(imageProvider, new OrientDBMeshOptions());
	}

	@Override
	protected void configure() {
		super.configure();

		if (!useFilesystem) {
			addEnv(GraphStorageOptions.MESH_GRAPH_DB_DIRECTORY_ENV, "null");
		} else {
			addEnv(GraphStorageOptions.MESH_GRAPH_DB_DIRECTORY_ENV, PATH_GRAPHDB);
		}
	}
}
