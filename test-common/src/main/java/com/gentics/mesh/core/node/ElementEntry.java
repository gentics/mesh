package com.gentics.mesh.core.node;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;

/***
 * Dummy container class used to conveniently store element references.
 */
public class ElementEntry {

	private String uuid;
	private List<String> languages;
	private SearchQueueEntryAction action;
	private String projectUuid;
	private String releaseUuid;
	private ContainerType type;

	/**
	 * Create a new entry
	 * @param action
	 * @param uuid
	 * @param projectUuid
	 * @param releaseUuid
	 * @param type
	 * @param languages
	 */
	public ElementEntry(SearchQueueEntryAction action, String uuid, String projectUuid, String releaseUuid, ContainerType type, List<String> languages) {
		this.action = action;
		this.uuid = uuid;
		this.projectUuid = projectUuid;
		this.releaseUuid = releaseUuid;
		this.type = type;
		this.languages = languages;
	}

	/**
	 * Create a new entry
	 * @param action
	 * @param uuid
	 * @param projectUuid
	 * @param releaseUuid
	 * @param type
	 * @param languages
	 */
	public ElementEntry(SearchQueueEntryAction action, String uuid, String projectUuid, String releaseUuid, ContainerType type, String... languages) {
		this(action, uuid, projectUuid, releaseUuid, type, Arrays.asList(languages));
	}

	/**
	 * Create a new entry.
	 * 
	 * @param action
	 * @param uuid
	 * @param languages
	 */
	public ElementEntry(SearchQueueEntryAction action, String uuid, List<String> languages) {
		this(action, uuid, null, null, null, languages);
	}

	/**
	 * Create a new entry.
	 * 
	 * @param action
	 * @param uuid
	 * @param languages
	 */
	public ElementEntry(SearchQueueEntryAction action, String uuid, String... languages) {
		this(action, uuid, null, null, null, Arrays.asList(languages));
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

	public String getProjectUuid() {
		return projectUuid;
	}

	public String getReleaseUuid() {
		return releaseUuid;
	}

	public ContainerType getType() {
		return type;
	}
}
