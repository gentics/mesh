package com.gentics.mesh.search.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

@Component
public abstract class AbstractIndexHandler<T extends MeshCoreVertex<?, T>> {

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

	public Observable<Void> update(T object) {
		return searchProvider.updateDocument(getIndex(), getType(), object.getUuid(), transformToDocumentMap(object));
	}

	public Observable<Void> update(String uuid, String type) {
		ObservableFuture<Void> fut = RxHelper.observableFuture();
		getRootVertex().findByUuid(uuid).map(element -> {
			if (element == null) {
				return Observable.error(new Exception("Element {" + uuid + "} for index type {" + type + "} could not be found within graph."));
			} else {
				return update(element);
			}
		});
		return fut;
	}

	public Observable<Void> store(T object, String type) {
		return searchProvider.storeDocument(getIndex(), type, object.getUuid(), transformToDocumentMap(object));
	}

	public Observable<Void> delete(String uuid, String type) {
		// We don't need to resolve the uuid and load the graph object in this case.
		return searchProvider.deleteDocument(getIndex(), type, uuid);
	}

	/**
	 * Load the given element and invoke store(T element) to store it in the index.
	 */
	public Observable<Void> store(String uuid, String indexType) {
		ObservableFuture<Void> obsFut = RxHelper.observableFuture();

		getRootVertex().findByUuid(uuid).subscribe(element -> {
			if (element == null) {
				obsFut.toHandler().handle(Future
						.failedFuture(new Exception("Element {" + uuid + "} for index type {" + indexType + "} could not be found within graph.")));
			} else {
				store(element, indexType).subscribe(done -> {
					obsFut.toHandler().handle(Future.succeededFuture(done));
				} , error -> {
					obsFut.toHandler().handle(Future.failedFuture(error));
				});
			}
		} , error -> {
			obsFut.toHandler().handle(Future.failedFuture(error));
		});
		return obsFut;
	}

	protected boolean isSearchClientAvailable() {
		return searchProvider != null;
	}

	/**
	 * Add basic references (creator, editor, created, edited) to the map for the given vertex.
	 * 
	 * @param map
	 * @param vertex
	 */
	protected void addBasicReferences(Map<String, Object> map, MeshCoreVertex<?, ?> vertex) {
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
		if (user != null) {
			// TODO make sure field names match response UserResponse field names..
			Map<String, Object> userFields = new HashMap<>();
			// For now we are not adding the user field to the indexed field since this would cause huge cascaded updates when the user object is being
			// modified.
			// userFields.put("username", user.getUsername());
			// userFields.put("emailadress", user.getEmailAddress());
			// userFields.put("firstname", user.getFirstname());
			// userFields.put("lastname", user.getLastname());
			// userFields.put("enabled", String.valueOf(user.isEnabled()));
			userFields.put("uuid", user.getUuid());
			map.put(key, userFields);
		}
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

	protected void addProject(Map<String, Object> map, Project project) {
		if (project != null) {
			Map<String, String> projectFields = new HashMap<>();
			projectFields.put("name", project.getName());
			projectFields.put("uuid", project.getUuid());
			map.put("project", projectFields);
		}
	}

	/**
	 * Handle a search index action. A action will modify the search index (delete, update, create)
	 * 
	 * @param uuid
	 *            Uuid of the document that should be handled
	 * @param actionName
	 *            Type of the action (delete, update, create)
	 * @param indexType
	 *            Type of the index
	 */
	public Observable<Void> handleAction(String uuid, String actionName, String indexType) {
		if (!isSearchClientAvailable()) {
			String msg = "Elasticsearch provider has not been initalized. It can't be used. Omitting search index handling!";
			log.error(msg);
			return Observable.error(new Exception(msg));
		}

		if (indexType == null) {
			indexType = getType();
		}
		SearchQueueEntryAction action = SearchQueueEntryAction.valueOfName(actionName);
		switch (action) {
		case CREATE_ACTION:
			return store(uuid, indexType);
		case DELETE_ACTION:
			return delete(uuid, indexType);
		case UPDATE_ACTION:
			// update(uuid, handler);
			return store(uuid, indexType);
		default:
			return Observable.error(new Exception("Action type {" + action + "} is unknown."));
		}
	}

}
