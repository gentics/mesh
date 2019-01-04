package com.gentics.mesh.core.data.changelog;

import com.gentics.mesh.core.data.changelog.ChangeMarkerVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

public class ChangeMarkerVertexImpl extends MeshVertexImpl implements ChangeMarkerVertex {

	/**
	 * Initialise the type and indices for this type.
	 * 
	 * @param database
	 */
	public static void init(LegacyDatabase database) {
		database.addVertexType(ChangeMarkerVertexImpl.class, MeshVertexImpl.class);
	}

}
