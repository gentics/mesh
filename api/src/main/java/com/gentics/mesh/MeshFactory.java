package com.gentics.mesh;

import com.gentics.mesh.etc.config.MeshOptions;

public interface MeshFactory {

	Mesh mesh();

	Mesh mesh(MeshOptions options);

}
