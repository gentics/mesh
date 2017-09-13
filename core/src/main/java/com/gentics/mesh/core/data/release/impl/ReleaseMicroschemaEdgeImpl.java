package com.gentics.mesh.core.data.release.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.release.ReleaseMicroschemaEdge;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.annotations.GraphElement;

/**
 * @see ReleaseMicroschemaEdge
 */
@GraphElement
public class ReleaseMicroschemaEdgeImpl extends AbstractVersionEdge implements ReleaseMicroschemaEdge {

	public static void init(Database db) {
		db.addEdgeType(ReleaseMicroschemaEdgeImpl.class.getSimpleName());
		db.addEdgeType(HAS_MICROSCHEMA_VERSION, ReleaseMicroschemaEdgeImpl.class);
	}

	@Override
	public MicroschemaContainerVersion getMicroschemaContainerVersion() {
		return inV().nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

}
