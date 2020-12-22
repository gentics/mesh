package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.search.index.AbstractMappingProvider.ROLE_UUIDS;
import static com.gentics.mesh.search.verticle.eventhandler.RxUtil.scrollAll;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.mesh.core.data.search.request.UpdateDocumentRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.verticle.MessageEvent;

import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Deletes all _roleUuid entries for the deleted role
 */
@Singleton
public class RoleDeletedEventHandler implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(RoleDeletedEventHandler.class);
	private static final int ELASTIC_SEARCH_PAGE_SIZE = 100;

	private final SearchProvider searchProvider;
	private final ComplianceMode complianceMode;

	@Inject
	public RoleDeletedEventHandler(SearchProvider searchProvider, AbstractMeshOptions options) {
		this.searchProvider = searchProvider;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Collections.singletonList(MeshEvent.ROLE_DELETED);
	}

	@Override
	public Flowable<UpdateDocumentRequest> handle(MessageEvent messageEvent) {
		MeshElementEventModelImpl model = requireType(MeshElementEventModelImpl.class, messageEvent.message);
		return getDocuments(model)
			.map(doc -> toUpdateRequest(model, doc));
	}

	private UpdateDocumentRequest toUpdateRequest(MeshElementEventModelImpl model, DocRoles doc) {
		JsonObject partial = new JsonObject()
			.put(ROLE_UUIDS,
				doc.roleUuids.stream()
					.filter(uuid -> !uuid.equals(model.getUuid()))
					.collect(Collectors.toList()));

		return new UpdateDocumentRequest(
			null,
			doc.index,
			doc.id,
			partial,
			complianceMode);
	}

	private Flowable<DocRoles> getDocuments(MeshElementEventModelImpl model) {
		ElasticsearchClient<JsonObject> client = searchProvider.getClient();
		// No client is set when using dev-null or tracking search provider
		if (client == null) {
			return Flowable.empty();
		}

		return scrollAll(client, createSearchQuery(model), "1m")
			.doOnNext(response -> {
				if (log.isTraceEnabled()) {
					log.trace("Found docs readable from role {}: {}", model.getUuid(), response);
				}
			})
			.concatMapIterable(response -> response.getJsonObject("hits").getJsonArray("hits"))
			.map(DocRoles::fromJson);
	}

	private JsonObject createSearchQuery(MeshElementEventModelImpl model) {
		return new JsonObject()
			.put("_source", ROLE_UUIDS)
			.put("size", ELASTIC_SEARCH_PAGE_SIZE)
			.put("query", new JsonObject()
				.put("term", new JsonObject()
					.put(ROLE_UUIDS, model.getUuid())));
	}

	private static class DocRoles {
		private final String index;
		private final String id;
		private final List<String> roleUuids;

		public DocRoles(String index, String id, List<String> roleUuids) {
			this.index = index;
			this.id = id;
			this.roleUuids = roleUuids;
		}

		private static DocRoles fromJson(Object json) {
			JsonObject obj = requireType(JsonObject.class, json);
			return new DocRoles(
				obj.getString("_index"),
				obj.getString("_id"),
				obj.getJsonObject("_source").getJsonArray(ROLE_UUIDS).getList());
		}
	}
}
