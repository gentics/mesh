package com.gentics.mesh.core.data.changelog;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;

public class ChangeMarkerVertexImpl extends MeshVertexImpl implements ChangeMarkerVertex {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ChangeMarkerVertexImpl.class, MeshVertexImpl.class);
	}

}
