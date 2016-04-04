package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
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
import com.gentics.mesh.core.data.UserTrackingVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
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
	 * Return the document type.
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
	 * Store the given object within the search index.
	 * 
	 * @param object
	 * @param documentType
	 * @return
	 */
	public Observable<Void> store(T object, String documentType) {
		return searchProvider.storeDocument(getIndex(), documentType, object.getUuid(), transformToDocumentMap(object)).doOnCompleted(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Stored object in index.");
			}
			MeshSpringConfiguration.getInstance().searchProvider().refreshIndex();
		});
	}

	@Override
	public Observable<Void> delete(String uuid, String documentType) {
		// We don't need to resolve the uuid and load the graph object in this case.
		return searchProvider.deleteDocument(getIndex(), documentType, uuid);
	}

	@Override
	public Observable<Void> store(String uuid, String indexType) {
		return getRootVertex().findByUuid(uuid).flatMap(element -> {
			return db.noTrx(() -> {
				if (element == null) {
					throw error(INTERNAL_SERVER_ERROR, "error_element_for_document_type_not_found", uuid, indexType);
				} else {
					return store(element, indexType);
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
		if (vertex instanceof UserTrackingVertex) {
			UserTrackingVertex trackedVertex = (UserTrackingVertex)vertex;
			addUser(map, "creator", trackedVertex.getCreator());
			addUser(map, "editor", trackedVertex.getEditor());
			map.put("edited", trackedVertex.getLastEditedTimestamp());
			map.put("created", trackedVertex.getCreationTimestamp());
		}
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
		case DELETE_ACTION:
			return delete(uuid, indexType);
		case STORE_ACTION:
			return store(uuid, indexType);
		case REINDEX_ALL:
			return reindexAll();
		default:
			return Observable.error(new Exception("Action type {" + action + "} is unknown."));
		}
	}

	@Override
	public Observable<Void> reindexAll() {
		log.info("Handling full reindex entry");
		for (T element : getRootVertex().findAll()) {
			log.info("Invoking reindex for {" + getType() + "/" + element.getUuid() + "}");
			SearchQueueBatch batch = element.createIndexBatch(STORE_ACTION);
			for (SearchQueueEntry entry : batch.getEntries()) {
				entry.process().toBlocking().lastOrDefault(null);
			}
			batch.delete();
		}
		return Observable.just(null);
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

	public Observable<Void> handleAction(SearchQueueEntry entry) {
		return handleAction(entry.getElementUuid(), entry.getElementActionName(), entry.getElementIndexType());
	}
}
