package com.gentics.mesh.search.index.entry;

import com.gentics.mesh.core.data.HandleContext;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;

/**
 * Abstract implementation for {@link SearchQueueEntry}'s. Please use this class if you want to build your own entry types.
 */
public abstract class AbstractEntry implements SearchQueueEntry {

	protected SearchQueueEntryAction elementAction;
	protected HandleContext context = new HandleContext();

	public AbstractEntry(SearchQueueEntryAction action) {
		this.elementAction = action;
	}

	@Override
	public SearchQueueEntryAction getElementAction() {
		return elementAction;
	}

	@Override
	public String toString() {
		return "action: {" + getElementAction().name() + "}";
	}

	@Override
	public HandleContext getContext() {
		return context;
	}

}
