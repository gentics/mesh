package com.gentics.mesh.search.index.schema;

import java.util.Collections;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.search.index.AbstractIndexHandler;

@Component
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

	private static SchemaContainerIndexHandler instance;

	private final static Set<String> indices = Collections.singleton("schema_container");

	private SchemaTransformator transformator = new SchemaTransformator();

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public SchemaTransformator getTransformator() {
		return transformator;
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

}
