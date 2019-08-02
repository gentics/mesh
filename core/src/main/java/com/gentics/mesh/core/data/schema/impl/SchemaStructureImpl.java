package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_STRUCTURE;

import java.util.Comparator;
import java.util.Optional;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaStructure;
import com.gentics.mesh.madl.traversal.TraversalResult;

public class SchemaStructureImpl extends MeshVertexImpl implements SchemaStructure {
	@Override
	public TraversalResult<SchemaContainerVersion> getSchemaContainerVersions() {
		return new TraversalResult<>(in(HAS_STRUCTURE).frameExplicit(SchemaContainerVersionImpl.class));
	}

	@Override
	public Optional<? extends SchemaContainerVersion> getLatestSchemaContainerVersion(String branchUuid) {
		return getSchemaContainerVersions().stream()
			.filter(schemaVersion -> schemaVersion.getBranches().stream().anyMatch(branch -> branch.getUuid().equals(branchUuid)))
			.max(Comparator.comparingDouble(schemaVersion -> Double.parseDouble(schemaVersion.getVersion())));
	}
}
