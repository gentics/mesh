package com.gentics.mesh.search.event;

public class RoleEventProcessor implements EventProcessor {

	@Override
	public void onCreated() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpdated() {
		// @Override
		// public void handleRelatedEntries(HandleElementAction action) {
		// for (Group group : getGroups()) {
		// action.call(group, null);
		// }
		// }

	}

	@Override
	public void onDeleted() {
		// Find and update all documents which list permissions on the deleted role
	}

}
