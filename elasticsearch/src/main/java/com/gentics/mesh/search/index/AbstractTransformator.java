package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.DATE;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;

import io.vertx.core.json.JsonObject;

/**
 * Abstract search document transformator which provides various helper methods which are commonly used among transformer implementations.
 *
 * @param <T>
 *            Type of the element which can be transformed
 */
public abstract class AbstractTransformator<T> implements Transformator<T> {

	/**
	 * Add basic references (creator, editor, created, edited) to the map for the given vertex.
	 * 
	 * @param document
	 *            JSON document to which basic references will be added
	 * @param vertex
	 *            Vertex which will be used to load the basic references
	 */
	protected void addBasicReferences(JsonObject document, MeshCoreVertex<?, ?> vertex) {
		document.put("uuid", vertex.getUuid());
		if (vertex instanceof CreatorTrackingVertex) {
			CreatorTrackingVertex createdVertex = (CreatorTrackingVertex) vertex;
			addUser(document, "creator", createdVertex.getCreator());
			document.put("created", createdVertex.getCreationDate());
		}
		if (vertex instanceof EditorTrackingVertex) {
			EditorTrackingVertex editedVertex = (EditorTrackingVertex) vertex;
			addUser(document, "editor", editedVertex.getEditor());
			document.put("edited", editedVertex.getLastEditedDate());
		}
	}

	/**
	 * Add the tags field to the source map using the given list of tags.
	 * 
	 * @param document
	 * @param tags
	 */
	public void addTags(JsonObject document, List<? extends Tag> tags) {
		List<String> tagUuids = new ArrayList<>();
		List<String> tagNames = new ArrayList<>();
		for (Tag tag : tags) {
			tagUuids.add(tag.getUuid());
			tagNames.add(tag.getName());
		}
		Map<String, List<String>> tagFields = new HashMap<>();
		tagFields.put("uuid", tagUuids);
		tagFields.put("name", tagNames);
		document.put("tags", tagFields);
	}

	/**
	 * Add a user field to the document with the given key.
	 * 
	 * @param document
	 * @param key
	 * @param user
	 */
	protected void addUser(JsonObject document, String key, User user) {
		if (user != null) {
			// TODO make sure field names match response UserResponse field names..
			JsonObject userFields = new JsonObject();
			userFields.put("uuid", user.getUuid());
			document.put(key, userFields);
		}
	}

	/**
	 * Add the project field to the source map.
	 * 
	 * @param document
	 * @param project
	 */
	protected void addProject(JsonObject document, Project project) {
		if (project != null) {
			Map<String, String> projectFields = new HashMap<>();
			projectFields.put("name", project.getName());
			projectFields.put("uuid", project.getUuid());
			document.put("project", projectFields);
		}
	}

	@Override
	public JsonObject getMapping(String type) {
		JsonObject mapping = new JsonObject();

		// Enhance mappings with generic/common field types
		JsonObject mappingProperties = getMappingProperties();
		mappingProperties.put(UUID_KEY, fieldType(STRING, NOT_ANALYZED));
		mappingProperties.put("created", fieldType(DATE, NOT_ANALYZED));
		mappingProperties.put("edited", fieldType(DATE, NOT_ANALYZED));
		mappingProperties.put("editor", getUserReferenceMapping());
		mappingProperties.put("creator", getUserReferenceMapping());

		JsonObject typeMapping = new JsonObject();
		typeMapping.put("properties", mappingProperties);

		mapping.put(type, typeMapping);
		return mapping;
	}

	/**
	 * Return the user reference mapping.
	 * 
	 * @return
	 */
	private JsonObject getUserReferenceMapping() {
		JsonObject mapping = new JsonObject();
		mapping.put("type", "object");
		JsonObject userProps = new JsonObject();
		userProps.put("uuid", fieldType(STRING, NOT_ANALYZED));
		mapping.put("properties", userProps);
		return mapping;
	}

}
