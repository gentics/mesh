package com.gentics.mesh.core.node;

import java.util.Arrays;
import java.util.List;

/***
 * Dummy container class used to conveniently store element references.
 */
public class ElementEntry {

	String uuid;
	List<String> languages;

	public ElementEntry(String uuid, String... languages) {
		this.uuid = uuid;
		this.languages = Arrays.asList(languages);
	}

	public String getUuid() {
		return uuid;
	}

	public List<String> getLanguages() {
		return languages;
	}

}
