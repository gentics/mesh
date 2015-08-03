package com.gentics.mesh.search.index;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;

@Component
public abstract class AbstractIndexHandler<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

	public static final String INDEX_EVENT_ADDRESS_PREFIX = "search-index-action-";

	@Autowired
	protected org.elasticsearch.node.Node elasticSearchNode;

	@Autowired
	protected BootstrapInitializer boot;

	@PostConstruct
	public void test() {
		Vertx vertx = Mesh.vertx();
		// Event handler that deals with new index events for this type.
		String address = INDEX_EVENT_ADDRESS_PREFIX + getType();
		log.info("Registering event handler for type {" + getType() + "} on address {" + address + "}");
		MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(address);
		consumer.handler(message -> {
			String uuid = message.body().getString("uuid");
			String type = message.body().getString("type");
			String action = message.body().getString("action");
			log.info("Handling index event for " + uuid + ":" + type + " event:" + action);
			handleEvent(uuid, action);
			message.reply(null);
		});

	}

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

	abstract public void update(String uuid);

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

	public void handleEvent(String uuid, String actionName) {
		SearchQueueEntryAction action = SearchQueueEntryAction.valueOfName(actionName);
		switch (action) {
		case CREATE_ACTION:
			store(uuid);
			break;
		case DELETE_ACTION:
			delete(uuid);
			break;
		case UPDATE_ACTION:
			update(uuid);
			break;
		}
	}

}
