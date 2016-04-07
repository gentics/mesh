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

import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueEntry;

import io.vertx.core.json.JsonObject;

@Component
public class MicroschemaContainerIndexHandler extends AbstractIndexHandler<MicroschemaContainer> {

	private static MicroschemaContainerIndexHandler instance;

	private final static Set<String> indices = Collections.singleton("microschema");

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static MicroschemaContainerIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return "microschema";
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
		return "microschema";
	}

	@Override
	public String getKey() {
		return MicroschemaContainer.TYPE;
	}

	@Override
	protected RootVertex<MicroschemaContainer> getRootVertex() {
		return boot.meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(MicroschemaContainer microschema) {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, microschema);
		map.put(NAME_KEY, microschema.getName());
		//map.put(DESCRIPTION_KEY, microschema.getSchema().getDescription());
		return map;
	}

	@Override
	protected JsonObject getMapping() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		props.put(DESCRIPTION_KEY, fieldType(STRING, ANALYZED));
		return props;
	}

}
