package com.gentics.mesh.core.data.branch.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.container.impl.MicroschemaVersionImpl;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;

/**
 * @see BranchMicroschemaEdge
 */
@GraphElement
public class BranchMicroschemaEdgeImpl extends AbstractVersionEdge implements BranchMicroschemaEdge {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(edgeType(BranchMicroschemaEdgeImpl.class.getSimpleName()));
		type.createType(edgeType(HAS_MICROSCHEMA_VERSION).withSuperClazz(BranchMicroschemaEdgeImpl.class));
	}

	@Override
	public MicroschemaVersion getMicroschemaContainerVersion() {
		return inV().nextOrDefaultExplicit(MicroschemaVersionImpl.class, null);
	}

}
