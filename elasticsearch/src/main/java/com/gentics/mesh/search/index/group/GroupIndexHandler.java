package com.gentics.mesh.search.index.group;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

/**
 * Handler for the elastic search group index.
 */
@Singleton
public class GroupIndexHandler extends AbstractIndexHandler<Group> {

	@Inject
	GroupTransformer transformer;

	@Inject
	public GroupIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	public Class<Group> getElementClass() {
		return Group.class;
	}

	@Override
	public GroupTransformer getTransformer() {
		return transformer;
	}

	@Override
	protected String composeIndexTypeFromEntry(UpdateDocumentEntry entry) {
		return Group.composeIndexType();
	}

	@Override
	public Map<String, String> getIndices() {
		return Collections.singletonMap(Group.TYPE, Group.TYPE);
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return entry.getElementUuid();
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return Group.composeIndexName();
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(Group.TYPE);
	}

	@Override
	public RootVertex<Group> getRootVertex() {
		return boot.meshRoot().getGroupRoot();
	}

}
