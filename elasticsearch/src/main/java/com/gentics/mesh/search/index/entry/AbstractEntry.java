package com.gentics.mesh.search.index.entry;

import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.context.EntryContext;

import io.reactivex.Observable;
import io.reactivex.functions.Action;

/**
 * Abstract implementation for {@link SearchQueueEntry}'s. Please use this class if you want to build your own entry types.
 */
public abstract class AbstractEntry<T extends EntryContext> implements SearchQueueEntry<T> {

	protected SearchQueueEntryAction elementAction;
	protected T context;
	protected Action onProcessAction = () -> {
	};

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
	public void setOnProcessAction(Action onProcessAction) {
		this.onProcessAction = onProcessAction;
	}

	@Override
	public T getContext() {
		return context;
	}
}
