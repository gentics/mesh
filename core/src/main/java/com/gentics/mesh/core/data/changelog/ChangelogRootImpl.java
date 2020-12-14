package com.gentics.mesh.core.data.changelog;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;

import java.util.Iterator;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;

/**
 * @see ChangelogRoot
 */
public class ChangelogRootImpl extends MeshVertexImpl implements ChangelogRoot {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ChangelogRootImpl.class, MeshVertexImpl.class);
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
		ChangeMarkerVertex marker = getGraph().addFramedVertex(ChangeMarkerVertexImpl.class);
		marker.setUuid(change.getUuid());
		marker.setDuration(duration);
		addFramedEdge(HAS_CHANGE, marker);
	}

}
