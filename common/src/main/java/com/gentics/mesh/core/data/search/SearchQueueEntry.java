package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.search.context.EntryContext;

import io.reactivex.functions.Action;

/**
 * A search queue entry is contains the information that is needed to update the search index for the element that is specified in this entry. In order to
 * update the search index various information are needed.
 * 
 * This includes:
 * <ul>
 * <li>Element UUUID - dd5e85cebb7311e49640316caf57479f</li>
 * <li>Element Type - node, tag, role</li>
 * <li>Element Action - delete, update, create</li>
 * <li>Element Context - ProjectUuid, BranchUuid</li>
 * </ul>
 */
public interface SearchQueueEntry<T extends EntryContext> extends Comparable<SearchQueueEntry<?>> {

	/**
	 * Return the search queue entry action (eg. Update, delete..)
	 * 
	 * @return
	 */
	SearchQueueEntryAction getElementAction();

	/**
	 * Return the context of the entry. The context contains information about the origin and scope of the action. This is later used to apply the desired
	 * action only to a specific index.
	 * 
	 * @return
	 */
	T getContext();

	/**
	 * Compare the given entry. The order of the element action will be used to compare the entries.
	 */
	default int compareTo(SearchQueueEntry<?> o) {
		return getElementAction().getPriority().compareTo(o.getElementAction().getPriority());
	}

	/**
	 * Action which will be invoked once the entry has been processed.
	 * 
	 * @param onProcessAction
	 */
	void setOnProcessAction(Action onProcessAction);

}
