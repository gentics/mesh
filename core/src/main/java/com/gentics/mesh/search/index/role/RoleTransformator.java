package com.gentics.mesh.search.index.role;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

public class RoleTransformator extends AbstractTransformator<Role> {

	/**
	 * Transform the given object into a source JSON object which can be used to store the document in the search provider specific format.
	 * 
	 * @param object
	 * @return
	 */
	public JsonObject toDocument(Role role) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, role.getName());
		addBasicReferences(document, role);
		return document;
	}

	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}

}
