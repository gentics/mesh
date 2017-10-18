package com.gentics.mesh.search.index.entry;

import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.search.context.EntryContext;

/**
 * Abstract implementation for {@link SearchQueueEntry}'s. Please use this class if you want to build your own entry types.
 */
public abstract class AbstractEntry<T extends EntryContext> implements SearchQueueEntry<T> {

	protected SearchQueueEntryAction elementAction;
	protected T context;

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
	public T getContext() {
		return context;
	}

}
