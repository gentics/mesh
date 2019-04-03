package com.gentics.mesh.core.node;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.ContainerType;

/***
 * Dummy container class used to conveniently store element references.
 */
public class ElementEntry {

	private String uuid;
	private List<String> languages;
	private SearchQueueEntryAction action;
	private String projectUuid;
	private String branchUuid;
	private ContainerType type;

	/**
	 * Create a new entry
	 * @param action
	 * @param uuid
	 * @param projectUuid
	 * @param branchUuid
	 * @param type
	 * @param languages
	 */
	public ElementEntry(SearchQueueEntryAction action, String uuid, String projectUuid, String branchUuid, ContainerType type, List<String> languages) {
		this.action = action;
		this.uuid = uuid;
		this.projectUuid = projectUuid;
		this.branchUuid = branchUuid;
		this.type = type;
		this.languages = languages;
	}

	/**
	 * Create a new entry
	 * @param action
	 * @param uuid
	 * @param projectUuid
	 * @param branchUuid
	 * @param type
	 * @param languages
	 */
	public ElementEntry(SearchQueueEntryAction action, String uuid, String projectUuid, String branchUuid, ContainerType type, String... languages) {
		this(action, uuid, projectUuid, branchUuid, type, Arrays.asList(languages));
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

	public String getBranchUuid() {
		return branchUuid;
	}

	public ContainerType getType() {
		return type;
	}
}
