package com.gentics.mesh.search.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;

@Component
public abstract class AbstractIndexHandler<T> {

	@Autowired
	protected org.elasticsearch.node.Node elasticSearchNode;

	@Autowired
	protected BootstrapInitializer boot;

	/**
	 * Transform the given element to a elasticsearch model and store it in the index.
	 * 
	 * @param element
	 * @throws IOException 
	 */
	abstract public void store(T element) throws IOException;

	abstract String getType();

	abstract String getIndex();

	/**
	 * Load the given element and invoke store(T element) to store it in the index.
	 * 
	 * @param uuid
	 */
	abstract public void store(String uuid);

	protected Client getClient() {
		return elasticSearchNode.client();
	}

	public void delete(String uuid) {
		DeleteResponse response = getClient().prepareDelete(getIndex(), getType(), uuid).execute().actionGet();
	}

	public void store(String uuid, Map<String, Object> map) {
		IndexResponse indexResponse = getClient().prepareIndex(getIndex(), getType(), uuid).setSource(map).execute().actionGet();
	}

	public void update(String uuid, Map<String, Object> map) {
		UpdateResponse updateResponse = getClient().prepareUpdate(getIndex(), getType(), uuid).setDoc(map).execute().actionGet();
	}

	protected void addBasicReferences(Map<String, Object> map, GenericVertex<?> vertex) {
		//TODO make sure field names match node response
		map.put("uuid", vertex.getUuid());
		addUser(map, "creator", vertex.getCreator());
		addUser(map, "editor", vertex.getEditor());
		map.put("lastEdited", vertex.getLastEditedTimestamp());
		map.put("created", vertex.getCreationTimestamp());
	}

	protected void addUser(Map<String, Object> map, String prefix, User user) {
		//TODO make sure field names match response UserResponse field names.. 
		Map<String, Object> userFields = new HashMap<>();
		userFields.put("username", user.getUsername());
		userFields.put("emailadress", user.getEmailAddress());
		userFields.put("firstname", user.getFirstname());
		userFields.put("lastname", user.getLastname());
		//TODO add disabled / enabled flag
		map.put(prefix, userFields);
	}

	protected void addTags(Map<String, Object> map, List<? extends Tag> tags) {

		List<String> tagUuids = new ArrayList<>();
		List<String> tagNames = new ArrayList<>();
		for (Tag tag : tags) {
			tagUuids.add(tag.getUuid());
			tagNames.add(tag.getName());
		}
		Map<String, List<String>> tagFields = new HashMap<>();
		tagFields.put("uuid", tagUuids);
		tagFields.put("name", tagNames);
		map.put("tags", tagFields);

	}

}
