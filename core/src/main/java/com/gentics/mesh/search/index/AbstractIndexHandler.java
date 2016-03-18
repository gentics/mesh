package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

@Component
public abstract class AbstractIndexHandler<T extends MeshCoreVertex<?, T>> implements IndexHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractIndexHandler.class);

	@Autowired
	protected SearchProvider searchProvider;

	@Autowired
	protected BootstrapInitializer boot;

	@Autowired
	protected Database db;

	@Autowired
	private IndexHandlerRegistry registry;

	@PostConstruct
	public void register() {
		registry.registerHandler(this);
	}

	/**
	 * Return the index type.
	 * 
	 * @return
	 */
	abstract protected String getType();

	/**
	 * Return the index name.
	 * 
	 * @return
	 */
	abstract protected String getIndex();

	/**
	 * Return the root vertex of the index handler. The root vertex is used to retrieve nodes by UUID in order to update the search index.
	 * 
	 * @return
	 */
	abstract protected RootVertex<T> getRootVertex();

	/**
	 * Transform the given object into a source map which can be used to store the document in the search provider specific format.
	 * 
	 * @param object
	 * @return
	 */
	abstract protected Map<String, Object> transformToDocumentMap(T object);

	/**
	 * Update the search index document which is represented by the given object.
	 * 
	 * @param object
	 * @param entry
	 * @return
	 */
	public Observable<Void> update(T object, SearchQueueEntry entry) {
		return searchProvider.updateDocument(getIndex(), getType(), object.getUuid(), transformToDocumentMap(object));
	}

	@Override
	public Observable<Void> update(String uuid, String type, SearchQueueEntry entry) {
		ObservableFuture<Void> fut = RxHelper.observableFuture();
		getRootVertex().findByUuid(uuid).map(element -> {
			if (element == null) {
				return Observable.error(new Exception("Element {" + uuid + "} for index type {" + type + "} could not be found within graph."));
			} else {
				return update(element, entry);
			}
		});
		return fut;
	}

	/**
	 * Store the given object within the search index.
	 * 
	 * @param object
	 * @param type
	 * @param entry
	 * @return
	 */
	public Observable<Void> store(T object, String type, SearchQueueEntry entry) {
		return searchProvider.storeDocument(getIndex(), type, object.getUuid(), transformToDocumentMap(object)).doOnCompleted(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Stored object in index.");
			}
			MeshSpringConfiguration.getInstance().searchProvider().refreshIndex();
		});
	}

	@Override
	public Observable<Void> delete(String uuid, String type, SearchQueueEntry entry) {
		// We don't need to resolve the uuid and load the graph object in this case.
		return searchProvider.deleteDocument(getIndex(), type, uuid);
	}

	@Override
	public Observable<Void> store(String uuid, String indexType, SearchQueueEntry entry) {
		return getRootVertex().findByUuid(uuid).flatMap(element -> {
			return db.noTrx(() -> {
				if (element == null) {
					throw error(INTERNAL_SERVER_ERROR, "error_element_for_index_type_not_found", uuid, indexType);
				} else {
					return store(element, indexType, entry);
				}
			});
		});
	}

	/**
	 * Check whether the search provider is available. Some tests are not starting an search provider and thus we must be able to determine whether we can use
	 * the search provider.
	 * 
	 * @return
	 */
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

	/**
	 * Add the tags field to the source map using the given list of tags.
	 * 
	 * @param map
	 * @param tags
	 */
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

	/**
	 * Add the project field to the source map.
	 * 
	 * @param map
	 * @param project
	 */
	protected void addProject(Map<String, Object> map, Project project) {
		if (project != null) {
			Map<String, String> projectFields = new HashMap<>();
			projectFields.put("name", project.getName());
			projectFields.put("uuid", project.getUuid());
			map.put("project", projectFields);
		}
	}

	@Override
	public Observable<Void> handleAction(SearchQueueEntry entry) {
		String uuid = entry.getElementUuid();
		String actionName = entry.getElementActionName();
		String indexType = entry.getElementIndexType();

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
			return store(uuid, indexType, entry);
		case DELETE_ACTION:
			return delete(uuid, indexType, entry);
		case UPDATE_ACTION:
			// update(uuid, handler);
			return store(uuid, indexType, entry);
		default:
			return Observable.error(new Exception("Action type {" + action + "} is unknown."));
		}
	}

	/**
	 * Return the index specific the mapping as JSON.
	 * 
	 * @return
	 */
	protected abstract JsonObject getMapping();

	@Override
	public Observable<Void> updateMapping() {
		try {
			PutMappingRequestBuilder mappingRequestBuilder = searchProvider.getNode().client().admin().indices().preparePutMapping(getIndex());
			mappingRequestBuilder.setType(getType());

			JsonObject mappingProperties = getMapping();
			// Enhance mappings with generic/common field types
			mappingProperties.put(UUID_KEY, fieldType(STRING, NOT_ANALYZED));
			JsonObject root = new JsonObject();
			root.put("properties", mappingProperties);
			JsonObject mapping = new JsonObject();
			mapping.put(getType(), root);

			mappingRequestBuilder.setSource(mapping.toString());

			ObservableFuture<Void> obs = RxHelper.observableFuture();
			mappingRequestBuilder.execute(new ActionListener<PutMappingResponse>() {

				@Override
				public void onResponse(PutMappingResponse response) {
					obs.toHandler().handle(Future.succeededFuture());
				}

				@Override
				public void onFailure(Throwable e) {
					obs.toHandler().handle(Future.failedFuture(e));
				}
			});
			return obs;
		} catch (Exception e) {
			return Observable.error(e);
		}
	}

	@Override
	public Observable<Void> createIndex() {
		return searchProvider.createIndex(getIndex());
	}

	@Override
	public Observable<Void> init() {
		return createIndex().flatMap(i -> updateMapping());
	}

	@Override
	public Observable<Void> clearIndex() {
		return searchProvider.clearIndex(getIndex());
	}
}
