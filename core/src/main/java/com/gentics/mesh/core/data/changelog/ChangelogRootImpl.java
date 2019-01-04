package com.gentics.mesh.core.data.changelog;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;

import java.util.Iterator;

import com.gentics.mesh.core.data.changelog.Change;
import com.gentics.mesh.core.data.changelog.ChangeMarkerVertex;
import com.gentics.mesh.core.data.changelog.ChangelogRoot;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

public class ChangelogRootImpl extends MeshVertexImpl implements ChangelogRoot {

	/**
	 * Initialise the type and indices for this type.
	 * 
	 * @param database
	 */
	public static void init(LegacyDatabase database) {
		database.addVertexType(ChangelogRootImpl.class, MeshVertexImpl.class);
	}

	@Override
	public Iterator<? extends ChangeMarkerVertex> findAll() {
		return out(HAS_CHANGE).frameExplicit(ChangeMarkerVertexImpl.class).iterator();
	}

	@Override
	public boolean hasChange(Change change) {
		return out(HAS_CHANGE).has("uuid", change.getUuid()).iterator().hasNext();
	}

	@Override
	public void add(Change change, long duration) {
		ChangeMarkerVertex marker = createVertex(ChangeMarkerVertexImpl.class);
		marker.setUuid(change.getUuid());
		marker.setDuration(duration);
		addFramedEdge(HAS_CHANGE, marker);
	}

}
