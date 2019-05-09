package com.gentics.mesh.search.event;

public class TagEventProcessor implements EventProcessor {

	@Override
	public void onCreated() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpdated() {

	}

	@Override
	public void onDeleted() {
		// TODO Auto-generated method stub

	}

	// @Override
	// public void handleRelatedEntries(HandleElementAction action) {
	// // Locate all nodes that use the tag across all branches and update these nodes
	// for (Branch branch : getProject().getBranchRoot().findAll()) {
	// for (Node node : getNodes(branch)) {
	// for (ContainerType type : Arrays.asList(ContainerType.DRAFT, ContainerType.PUBLISHED)) {
	// GenericEntryContextImpl context = new GenericEntryContextImpl();
	// context.setContainerType(type);
	// context.setBranchUuid(branch.getUuid());
	// context.setProjectUuid(node.getProject().getUuid());
	// action.call(node, context);
	// }
	// }
	// }
	// }

}
