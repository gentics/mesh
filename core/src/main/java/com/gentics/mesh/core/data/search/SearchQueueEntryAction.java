package com.gentics.mesh.core.data.search;

/**
 * A search queue entry action defines how the index search should be modified.
 */
public enum SearchQueueEntryAction {

	DELETE_ACTION("delete", 5),

	REINDEX_ALL("reindex_all", 3),

	STORE_ACTION("store", 4),

	CREATE_INDEX("create_index", 0),

	UPDATE_MAPPING("update_mapping", 1);

	private String name;

	private int order;

	/**
	 * Create a new action.
	 * 
	 * @param name
	 *            Name of the action
	 * @param order
	 *            Order of the action. A higher order means that the action will be execute later.
	 */
	private SearchQueueEntryAction(String name, int order) {
		this.name = name;
		this.order = order;
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
	 * Return the order of the action. Lower order means the action should be executed earlier compared to entries with higher order.
	 * 
	 * @return
	 */
	public Integer getOrder() {
		return new Integer(order);
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

	/**
	 * Compare the order of both entries.
	 * 
	 * @param o
	 * @return
	 */
	public int compareOrder(SearchQueueEntryAction o) {
		//TODO handle null
		return getOrder().compareTo(o.getOrder());
	}

}
