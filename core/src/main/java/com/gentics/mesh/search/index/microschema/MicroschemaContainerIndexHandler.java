package com.gentics.mesh.search.index.microschema;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.dagger.MeshCore;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

public class MicroschemaContainerIndexHandler extends AbstractIndexHandler<MicroschemaContainer> {

	private final static Set<String> indices = Collections.singleton("microschema");

	private MicroschemaTransformator transformator = new MicroschemaTransformator();

	@Inject
	public MicroschemaContainerIndexHandler(SearchProvider searchProvider, Database db) {
		super(searchProvider, db);
	}

	public MicroschemaTransformator getTransformator() {
		return transformator;
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
		return MeshCore.get().boot().meshRoot().getMicroschemaContainerRoot();
	}

}
