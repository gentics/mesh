package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.DESCRIPTION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.rest.schema.Schema;

import io.vertx.core.json.JsonObject;
import rx.Completable;

@Component
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

	private static SchemaContainerIndexHandler instance;

	private final static Set<String> indices = Collections.singleton("schema_container");

	@Autowired
	private NodeIndexHandler nodeIndexHandler;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static SchemaContainerIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return "schema_container";
	}

	@Override
	public Set<String> getIndices() {
		return indices;
	}

	@Override
	public Set<String> getAffectedIndices(InternalActionContext ac) {
		return indices;
	}

	@Override
	protected String getType() {
		return "schemaContainer";
	}

	@Override
	public String getKey() {
		return SchemaContainer.TYPE;
	}

	@Override
	protected RootVertex<SchemaContainer> getRootVertex() {
		return boot.meshRoot().getSchemaContainerRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(SchemaContainer container) {
		Map<String, Object> map = new HashMap<>();
		map.put(NAME_KEY, container.getName());
		map.put(DESCRIPTION_KEY, container.getLatestVersion().getSchema().getDescription());
		addBasicReferences(map, container);
		return map;
	}

	@Override
	public Completable store(SchemaContainer container, String type, SearchQueueEntry entry) {
		return super.store(container, type, entry).andThen(Completable.defer(()-> {
			if (db != null) {
				return db.noTrx(() -> {
					// update the mappings
					Schema schema = container.getLatestVersion().getSchema();
					return nodeIndexHandler.setNodeIndexMapping(NodeIndexHandler.getDocumentType(container.getLatestVersion()), schema);
				});
			} else {
				return Completable.complete();
			}
		}));
	}

	@Override
	protected JsonObject getMapping() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		props.put(DESCRIPTION_KEY, fieldType(STRING, ANALYZED));
		return props;
	}
}
