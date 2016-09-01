package com.gentics.mesh.search.index.group;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

/**
 * Handler for the elastic search group index
 */
public class GroupIndexHandler extends AbstractIndexHandler<Group> {

	private GroupTransformator transformator = new GroupTransformator();

	@Inject
	public GroupIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot) {
		super(searchProvider, db, boot);
	}

	public GroupTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return Group.TYPE;
	}

	@Override
	protected String getDocumentType(SearchQueueEntry entry) {
		// The document type for groups is not entry specific.
		return Group.TYPE;
	}

	@Override
	public Set<String> getAffectedIndices(InternalActionContext ac) {
		return Collections.singleton(Group.TYPE);
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
