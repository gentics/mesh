package com.gentics.mesh.search.event;

public class SchemaEventProcessor implements EventProcessor {

	@Override
	public void onCreated() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpdated() {
		// batch.createNodeIndex(projectUuid, branchUuid, schemaContainerVersion.getUuid(), DRAFT, schemaContainerVersion.getSchema());
		// batch.createNodeIndex(projectUuid, branchUuid, schemaContainerVersion.getUuid(), PUBLISHED, schemaContainerVersion.getSchema());
	}

	@Override
	public void onDeleted() {
		// TODO Auto-generated method stub

	}

}
