package com.gentics.mesh.core.data.changelog;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;

public class ChangeMarkerVertexImpl extends MeshVertexImpl implements ChangeMarkerVertex {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ChangeMarkerVertexImpl.class, MeshVertexImpl.class);
	}

}
