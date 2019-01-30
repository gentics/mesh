package com.gentics.mesh.search.event;

public class ProjectEventProcessor implements EventProcessor {

	@Override
	public void onCreated() {
		// TODO Auto-generated method stub
		// 1. Create needed indices
		// batch.createNodeIndex(projectUuid, branchUuid, schemaContainerVersion.getUuid(), DRAFT, schemaContainerVersion.getSchema());
		// batch.createNodeIndex(projectUuid, branchUuid, schemaContainerVersion.getUuid(), PUBLISHED, schemaContainerVersion.getSchema());
		// batch.createTagIndex(projectUuid);
		// batch.createTagFamilyIndex(projectUuid);

	}

	@Override
	public void onUpdated() {
		// TODO Auto-generated method stub

		// @Override
		// public void handleRelatedEntries(HandleElementAction action) {
		// // Check whether a base node exits. The base node may have been deleted.
		// // In that case we can't handle related entries
		// if (getBaseNode() == null) {
		// return;
		// }
		// // All nodes of all branches are related to this project. All
		// // nodes/containers must be updated if the project name changes.
		// for (Node node : getNodeRoot().findAll()) {
		// action.call(node, new GenericEntryContextImpl());
		// }
		//
		// for (TagFamily family : getTagFamilyRoot().findAll()) {
		// for (Tag tag : family.findAll()) {
		// action.call(tag, new GenericEntryContextImpl().setProjectUuid(getUuid()));
		// }
		// }
		//
		// for (TagFamily tagFamily : getTagFamilyRoot().findAll()) {
		// action.call(tagFamily, new GenericEntryContextImpl().setProjectUuid(getUuid()));
		// }
		// }

	}

	@Override
	public void onDeleted() {
		// Set<String> indices = new HashSet<>();
		// // Drop all node indices for all releases and all schema versions
		// for (Branch branch : getBranchRoot().findAll()) {
		// for (SchemaContainerVersion version : branch.findActiveSchemaVersions()) {
		// for (ContainerType type : Arrays.asList(DRAFT, PUBLISHED)) {
		// String index = NodeGraphFieldContainer.composeIndexName(getUuid(), branch.getUuid(), version.getUuid(), type);
		// if (log.isDebugEnabled()) {
		// log.debug("Adding drop entry for index {" + index + "}");
		// }
		// indices.add(index);
		// }
		// }
		// }
		//
		//
		// for (String index : indices) {
		// bac.dropIndex(index);
		// }

		// Drop the project specific indices
		// bac.dropIndex(TagFamily.composeIndexName(getUuid()));
		// bac.dropIndex(Tag.composeIndexName(getUuid()));

	}

}
