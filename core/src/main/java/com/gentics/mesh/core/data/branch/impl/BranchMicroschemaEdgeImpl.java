package com.gentics.mesh.core.data.branch.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_VERSION;
import static com.syncleus.ferma.type.EdgeTypeDefinition.edgeType;

import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;
import com.syncleus.ferma.annotations.GraphElement;

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
	public MicroschemaContainerVersion getMicroschemaContainerVersion() {
		return inV().nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

}
