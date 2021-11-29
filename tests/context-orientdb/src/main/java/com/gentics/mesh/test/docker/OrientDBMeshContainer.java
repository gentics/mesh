package com.gentics.mesh.test.docker;

import java.util.function.Function;

import org.testcontainers.images.builder.ImageFromDockerfile;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

public class OrientDBMeshContainer extends MeshContainer {

	public OrientDBMeshContainer(String imageName) {
		super(imageName);
	}

	public OrientDBMeshContainer(Function<MeshOptions, ImageFromDockerfile> imageProvider) {
		super(imageProvider, new OrientDBMeshOptions());
	}
}
