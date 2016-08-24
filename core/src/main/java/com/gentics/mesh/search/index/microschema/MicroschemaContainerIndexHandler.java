package com.gentics.mesh.search.index.microschema;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

public class MicroschemaContainerIndexHandler extends AbstractIndexHandler<MicroschemaContainer> {

	private static MicroschemaContainerIndexHandler instance;

	private final static Set<String> indices = Collections.singleton("microschema");

	private MicroschemaTransformator transformator = new MicroschemaTransformator();

	private BootstrapInitializer boot;

	@Inject
	public MicroschemaContainerIndexHandler(SearchProvider searchProvider, Database db, IndexHandlerRegistry registry, BootstrapInitializer boot) {
		super(searchProvider, db, registry);
		this.boot = boot;
		instance = this;
	}

	public static MicroschemaContainerIndexHandler getInstance() {
		return instance;
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
		return boot.meshRoot().getMicroschemaContainerRoot();
	}

}
