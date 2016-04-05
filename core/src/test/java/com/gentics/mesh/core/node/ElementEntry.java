package com.gentics.mesh.core.node;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.core.data.search.SearchQueueEntryAction;

/***
 * Dummy container class used to conveniently store element references.
 */
public class ElementEntry {

	private String uuid;
	private List<String> languages;
	private SearchQueueEntryAction action;

	/**
	 * Create a new entry.
	 * 
	 * @param action
	 * @param uuid
	 * @param languages
	 */
	public ElementEntry(SearchQueueEntryAction action, String uuid, List<String> languages) {
		this.action = action;
		this.uuid = uuid;
		this.languages = languages;
	}

	/**
	 * Create a new entry.
	 * 
	 * @param action
	 * @param uuid
	 * @param languages
	 */
	public ElementEntry(SearchQueueEntryAction action, String uuid, String... languages) {
		this(action, uuid, Arrays.asList(languages));
	}

	public SearchQueueEntryAction getAction() {
		return action;
	}

	public String getUuid() {
		return uuid;
	}

	public List<String> getLanguages() {
		return languages;
	}

}
