package com.gentics.mesh.search.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public abstract class AbstractIndexHandler<T extends GenericVertex<?>> {

	private static final Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

	@Autowired
	protected SearchProvider searchProvider;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected Database db;

	abstract protected String getType();

	abstract protected String getIndex();

	abstract protected RootVertex<T> getRootVertex();

	abstract protected Map<String, Object> transformToDocumentMap(T object);

	public void update(T object, Handler<AsyncResult<Void>> handler) {
		searchProvider.updateDocument(getIndex(), getType(), object.getUuid(), transformToDocumentMap(object), handler);
	}

	public void update(String uuid, String indexType, Handler<AsyncResult<Void>> handler) {
		getRootVertex().findByUuid(uuid, rh -> {
			if (rh.failed()) {
				handler.handle(Future.failedFuture(rh.cause()));
			} else if (rh.result() == null) {
				handler.handle(Future.failedFuture("Element {" + uuid + "} for index type {" + getType() + "} could not be found within graph."));
			} else {
				update(rh.result(), handler);
			}
		});
	}

	public void store(T object, Handler<AsyncResult<Void>> handler) {
		searchProvider.storeDocument(getIndex(), getType(), object.getUuid(), transformToDocumentMap(object), handler);
	}

	public void delete(String uuid, String indexType, Handler<AsyncResult<Void>> handler) {
		// We don't need to resolve the uuid and load the graph object in this case.
		searchProvider.deleteDocument(getIndex(), getType(), uuid, handler);
	}

	/**
	 * Load the given element and invoke store(T element) to store it in the index.
	 */
	public void store(String uuid, String indexType, Handler<AsyncResult<Void>> handler) {
		getRootVertex().findByUuid(uuid, rh -> {
			if (rh.failed()) {
				handler.handle(Future.failedFuture(rh.cause()));
			} else if (rh.result() == null) {
				handler.handle(Future.failedFuture("Element {" + uuid + "} for index type {" + indexType + "} could not be found within graph."));
			} else {
				store(rh.result(), handler);
			}
		});
	}

	protected boolean isSearchClientAvailable() {
		return searchProvider != null;
	}

	protected void addBasicReferences(Map<String, Object> map, GenericVertex<?> vertex) {
		// TODO make sure field names match node response
		map.put("uuid", vertex.getUuid());
		addUser(map, "creator", vertex.getCreator());
		addUser(map, "editor", vertex.getEditor());
		map.put("edited", vertex.getLastEditedTimestamp());
		map.put("created", vertex.getCreationTimestamp());
	}

	/**
	 * Add a user field to the map with the given key.
	 * 
	 * @param map
	 * @param key
	 * @param user
	 */
	protected void addUser(Map<String, Object> map, String key, User user) {
		// TODO make sure field names match response UserResponse field names..
		Map<String, Object> userFields = new HashMap<>();
		// For now we are not adding the user field to the indexed field since this would cause huge cascaded updates when the user object is being modified.
		// userFields.put("username", user.getUsername());
		// userFields.put("emailadress", user.getEmailAddress());
		// userFields.put("firstname", user.getFirstname());
		// userFields.put("lastname", user.getLastname());
		// userFields.put("enabled", String.valueOf(user.isEnabled()));
		userFields.put("uuid", user.getUuid());
		map.put(key, userFields);
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

	public void handleAction(String uuid, String actionName, String indexType, Handler<AsyncResult<Void>> handler) {
		if (!isSearchClientAvailable()) {
			String msg = "Elasticsearch provider has not been initalized. It can't be used. Omitting search index handling!";
			log.error(msg);
			handler.handle(Future.failedFuture(msg));
			return;
		}

		if (indexType == null) {
			indexType = getType();
		}
		SearchQueueEntryAction action = SearchQueueEntryAction.valueOfName(actionName);
		// try (Trx tx = db.trx()) {
		switch (action) {
		case CREATE_ACTION:
			store(uuid, indexType, handler);
			break;
		case DELETE_ACTION:
			delete(uuid, indexType, handler);
			break;
		case UPDATE_ACTION:
			// update(uuid, handler);
			store(uuid, indexType, handler);
			break;
		default:
			handler.handle(Future.failedFuture("Action type {" + action + "} is unknown."));
		}
		// }
	}

}
