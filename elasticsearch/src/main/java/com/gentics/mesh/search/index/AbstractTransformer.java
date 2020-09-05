package com.gentics.mesh.search.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract search document transformer which provides various helper methods which are commonly used among transformer implementations.
 *
 * @param <T>
 *            Type of the element which can be transformed
 */
public abstract class AbstractTransformer<T> implements Transformer<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractTransformer.class);

	public static final String VERSION_KEY = "version";
	public static final int MAX_RAW_FIELD_LEN = 32_700;

	/**
	 * Truncate the field to the 32KB boundary for tokens within the lucene.
	 * 
	 * @param input
	 * @return Truncated string
	 */
	protected String truncateRawFieldValue(String input) {
		if (input == null) {
			return null;
		}
		if (input.length() > MAX_RAW_FIELD_LEN) {
			if (log.isDebugEnabled()) {
				log.debug("String with len {" + input.length() + "} is too long. Truncating it to {" + MAX_RAW_FIELD_LEN + "}");
			}
			return input.substring(0, MAX_RAW_FIELD_LEN);
		} else {
			return input;
		}
	}

	/**
	 * Add basic references (creator, editor, created, edited) to the map for the given vertex.
	 * 
	 * @param document
	 *            JSON document to which basic references will be added
	 * @param element
	 *            Element which will be used to load the basic references
	 */
	protected void addBasicReferences(JsonObject document, HibBaseElement element) {
		document.put("uuid", element.getUuid());
		if (element instanceof CreatorTrackingVertex) {
			CreatorTrackingVertex createdVertex = (CreatorTrackingVertex) element;
			addUser(document, "creator", createdVertex.getCreator());
			document.put("created", createdVertex.getCreationDate());
		}
		if (element instanceof EditorTrackingVertex) {
			EditorTrackingVertex editedVertex = (EditorTrackingVertex) element;
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
	public void addTags(JsonObject document, Iterable<? extends HibTag> tags) {
		List<String> tagUuids = new ArrayList<>();
		List<String> tagNames = new ArrayList<>();
		for (HibTag tag : tags) {
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
	protected void addUser(JsonObject document, String key, HibUser user) {
		if (user != null) {
			// TODO make sure field names match response UserResponse field names..
			JsonObject userFields = new JsonObject();
			userFields.put("uuid", user.getUuid());
			document.put(key, userFields);
		}
	}

	/**
	 * Adds the information which roles can read permission the given element to the document. This information will later be used by the permission script to
	 * filter out document which should not be visible to the user which invokes the query.
	 * 
	 * @param document
	 * @param element
	 */
	protected void addPermissionInfo(JsonObject document, HibBaseElement element) {
		Set<String> roleUuids = element.getRoleUuidsForPerm(InternalPermission.READ_PERM);
		List<String> roleUuidsList = new ArrayList<>(roleUuids);
		document.put("_roleUuids", roleUuidsList);
	}

	@Override
	public JsonObject toPermissionPartial(HibBaseElement element) {
		JsonObject document = new JsonObject();
		addPermissionInfo(document, element);
		return document;
	}

	/**
	 * Add the project field to the source map.
	 * 
	 * @param document
	 * @param project
	 */
	protected void addProject(JsonObject document, HibProject project) {
		if (project != null) {
			Map<String, String> projectFields = new HashMap<>();
			projectFields.put("name", project.getName());
			projectFields.put("uuid", project.getUuid());
			document.put("project", projectFields);
		}
	}

}
