package com.gentics.mesh.search.event;

import com.gentics.mesh.core.data.HandleElementAction;
import com.gentics.mesh.core.data.User;

public class GroupEventProcessor implements EventProcessor {

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

	// CUD on groups also need to update user documents in the index
	// @Override
	// public void handleRelatedEntries(HandleElementAction action) {
	// for (User user : getUsers()) {
	// // We need to store users as well since users list their groups -
	// // See {@link UserTransformer#toDocument(User)}
	// action.call(user, null);
	// }
	// }

}
