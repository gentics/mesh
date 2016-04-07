package com.gentics.mesh.core.data.search;

/**
 * A search queue entry action defines how the index search should be modified.
 */
public enum SearchQueueEntryAction {
	DELETE_ACTION("delete"),
	REINDEX_ALL("reindex_all"), 
	STORE_ACTION("store"),
	CREATE_INDEX("create_index");

	private String name;

	private SearchQueueEntryAction(String name) {
		this.name = name;
	}

	/**
	 * Return the name of the action.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the {@link SearchQueueEntryAction} for the given action name.
	 * 
	 * @param actionName
	 * @return Resolved enum or null when the name could not be resolved
	 */
	public static SearchQueueEntryAction valueOfName(String actionName) {
		for (SearchQueueEntryAction action : SearchQueueEntryAction.values()) {
			if (actionName.equals(action.getName())) {
				return action;
			}
		}
		return null;
	}

}
