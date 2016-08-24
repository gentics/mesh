package com.gentics.mesh.search.index.group;

import java.util.Collections;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

public class GroupIndexHandler extends AbstractIndexHandler<Group> {

	private static GroupIndexHandler instance;

	private final static Set<String> indices = Collections.singleton("group");

	private GroupTransformator transformator = new GroupTransformator();

	private BootstrapInitializer boot;

	public GroupIndexHandler(BootstrapInitializer boot, SearchProvider searchProvider, Database db, IndexHandlerRegistry registry) {
		super(searchProvider, db, registry);
		this.boot = boot;
		instance = this;
	}

	public static GroupIndexHandler getInstance() {
		return instance;
	}

	public GroupTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return "group";
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
		return "group";
	}

	@Override
	public String getKey() {
		return Group.TYPE;
	}

	@Override
	protected RootVertex<Group> getRootVertex() {
		return boot.meshRoot().getGroupRoot();
	}

}
