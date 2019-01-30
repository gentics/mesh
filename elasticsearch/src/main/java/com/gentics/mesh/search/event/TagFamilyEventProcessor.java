package com.gentics.mesh.search.event;

public class TagFamilyEventProcessor implements EventProcessor {

	@Override
	public void onCreated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUpdated() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleted() {
		// TODO Auto-generated method stub
		
	}

	// @Override
	// public void handleRelatedEntries(HandleElementAction action) {
	// for (Tag tag : findAll()) {
	// GenericEntryContextImpl context = new GenericEntryContextImpl();
	// context.setProjectUuid(tag.getProject().getUuid());
	// action.call(tag, context);
	//
	// // To prevent nodes from being handled multiple times
	// HashSet<String> handledNodes = new HashSet<>();
	//
	// for (Branch branch : tag.getProject().getBranchRoot().findAll()) {
	// for (Node node : tag.getNodes(branch)) {
	// if (!handledNodes.contains(node.getUuid())) {
	// handledNodes.add(node.getUuid());
	// GenericEntryContextImpl nodeContext = new GenericEntryContextImpl();
	// context.setBranchUuid(branch.getUuid());
	// context.setProjectUuid(node.getProject().getUuid());
	// action.call(node, nodeContext);
	// }
	// }
	// }
	// }
	// }

}
