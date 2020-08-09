package com.gentics.mesh.core.data.search;

/**
 * A search queue entry action defines how the index search should be modified.
 */
public enum SearchQueueEntryAction {

	DROP_INDEX("drop_index", 100),

	CREATE_INDEX("create_index", 90),

	DELETE_ACTION("delete", 80),

	UPDATE_ROLE_PERM_ACTION("updateRolePerm", 70),

	STORE_ACTION("store", 60),

	MOVE_ACTION("move", 50);

	private String name;

	private int priority;

	/**
	 * Create a new action.
	 * 
	 * @param name
	 *            Name of the action
	 * @param priority
	 *            Priority of the action. A higher priority means that the action will be executed earlier.
	 */
	private SearchQueueEntryAction(String name, int priority) {
		this.name = name;
		this.priority = priority;
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
	 * Return the priority of the action. Lower priority means the action should be executed laster compared to entries with higher priority.
	 * 
	 * @return
	 */
	public Integer getPriority() {
		return new Integer(priority);
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
		// TODO handle null
		return getPriority().compareTo(o.getPriority());
	}

}
