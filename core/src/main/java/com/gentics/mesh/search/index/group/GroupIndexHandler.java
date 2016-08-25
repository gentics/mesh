package com.gentics.mesh.search.index.group;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.dagger.MeshCore;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

public class GroupIndexHandler extends AbstractIndexHandler<Group> {

	private final static Set<String> indices = Collections.singleton("group");

	private GroupTransformator transformator = new GroupTransformator();

	@Inject
	public GroupIndexHandler(SearchProvider searchProvider, Database db) {
		super(searchProvider, db);
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
		return MeshCore.get().boot().meshRoot().getGroupRoot();
	}

}
