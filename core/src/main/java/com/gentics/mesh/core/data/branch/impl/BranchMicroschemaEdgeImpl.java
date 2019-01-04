package com.gentics.mesh.core.data.branch.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;

import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.madl.annotation.GraphElement;

/**
 * @see BranchMicroschemaEdge
 */
@GraphElement
public class BranchMicroschemaEdgeImpl extends AbstractVersionEdge implements BranchMicroschemaEdge {

	public static void init(LegacyDatabase db) {
		db.addEdgeType(BranchMicroschemaEdgeImpl.class.getSimpleName());
		db.addEdgeType(HAS_MICROSCHEMA_VERSION, BranchMicroschemaEdgeImpl.class);
	}

	@Override
	public MicroschemaContainerVersion getMicroschemaContainerVersion() {
		return inV().nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

}
