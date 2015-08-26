package com.gentics.mesh.search.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
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
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public abstract class AbstractIndexHandler<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

	public static final String INDEX_EVENT_ADDRESS_PREFIX = "search-index-action-";

	@Autowired
	protected org.elasticsearch.node.Node elasticSearchNode;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected Database db;

	@PostConstruct
	public void registerEventHandler() {
		Vertx vertx = Mesh.vertx();
		// Event handler that deals with new index events for this type.
		String address = INDEX_EVENT_ADDRESS_PREFIX + getType();
		log.info("Registering event handler for type {" + getType() + "} on address {" + address + "}");
		MessageConsumer<JsonObject> consumer = vertx.eventBus().consumer(address);
		consumer.handler(message -> {
			String uuid = message.body().getString("uuid");
			String type = message.body().getString("type");
			String action = message.body().getString("action");
			log.info("Handling index event for {" + uuid + ":" + type + "} event:" + action);
			handleEvent(uuid, action, rh -> {
				if (rh.succeeded()) {
					message.reply(null);
				} else {
					message.fail(500, rh.cause().getMessage());
				}
			});
		});

	}

	/**
	 * Transform the given element to a elasticsearch model and store it in the index.
	 * 
	 * @param element
	 * @param handler
	 * @throws IOException
	 */
	abstract public void store(T element, Handler<AsyncResult<ActionResponse>> handler) throws IOException;

	abstract String getType();

	abstract String getIndex();

	/**
	 * Load the given element and invoke store(T element) to store it in the index.
	 * 
	 * @param uuid
	 */
	abstract public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler);

	abstract public void update(String uuid, Handler<AsyncResult<ActionResponse>> handler);

	protected Client getSearchClient() {
		return elasticSearchNode.client();
	}

	public void delete(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		getSearchClient().prepareDelete(getIndex(), getType(), uuid).execute().addListener(new ActionListener<DeleteResponse>() {

			@Override
			public void onResponse(DeleteResponse response) {
				//TODO log
				handler.handle(Future.succeededFuture(response));
			}

			@Override
			public void onFailure(Throwable e) {
				//TODO log
				handler.handle(Future.failedFuture(e));
			}
		});
	}

	protected void update(String uuid, Map<String, Object> map, String type, Handler<AsyncResult<ActionResponse>> handler) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Updating object {" + uuid + ":" + getType() + "} to index.");
		}
		UpdateRequestBuilder builder = getSearchClient().prepareUpdate(getIndex(), type, uuid);
		builder.setDoc(map);
		builder.execute().addListener(new ActionListener<UpdateResponse>() {

			@Override
			public void onResponse(UpdateResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Update object {" + uuid + ":" + getType() + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				handler.handle(Future.succeededFuture(response));

			}

			@Override
			public void onFailure(Throwable e) {
				log.error(
						"Updating object {" + uuid + ":" + getType() + "} to index failed. Duration " + (System.currentTimeMillis() - start) + "[ms]",
						e);
				handler.handle(Future.failedFuture(e));
			}
		});

	}

	protected void store(String uuid, Map<String, Object> map, Handler<AsyncResult<ActionResponse>> handler) {
		long start = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Adding object {" + uuid + ":" + getType() + "} to index.");
		}
		IndexRequestBuilder builder = getSearchClient().prepareIndex(getIndex(), getType(), uuid);
		builder.setSource(map);
		builder.execute().addListener(new ActionListener<IndexResponse>() {

			@Override
			public void onResponse(IndexResponse response) {
				if (log.isDebugEnabled()) {
					log.debug("Added object {" + uuid + ":" + getType() + "} to index. Duration " + (System.currentTimeMillis() - start) + "[ms]");
				}
				handler.handle(Future.succeededFuture(response));

			}

			@Override
			public void onFailure(Throwable e) {
				if (log.isDebugEnabled()) {
					log.error("Adding object {" + uuid + ":" + getType() + "} to index failed. Duration " + (System.currentTimeMillis() - start)
							+ "[ms]", e);
				}
				handler.handle(Future.failedFuture(e));
			}
		});

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

	public void handleEvent(String uuid, String actionName, Handler<AsyncResult<ActionResponse>> handler) {
		SearchQueueEntryAction action = SearchQueueEntryAction.valueOfName(actionName);
		try (Trx tx = new Trx(db)) {
			switch (action) {
			case CREATE_ACTION:
				store(uuid, handler);
				break;
			case DELETE_ACTION:
				delete(uuid, handler);
				break;
			case UPDATE_ACTION:
				update(uuid, handler);
				break;
			}
		}
	}

}
