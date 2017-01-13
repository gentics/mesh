package com.gentics.mesh.search.index.user;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateBatchEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

@Singleton
public class UserIndexHandler extends AbstractIndexHandler<User> {

	private final static Set<String> indices = Collections.singleton(User.TYPE);

	@Inject
	UserTransformator transformator;

	@Inject
	public UserIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	protected Class<User> getElementClass() {
		return User.class;
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateBatchEntry entry) {
		return User.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateBatchEntry entry) {
		return User.composeIndexName();
	}

	@Override
	protected String composeIndexTypeFromEntry(UpdateBatchEntry entry) {
		return User.composeIndexType();
	}

	@Override
	public UserTransformator getTransformator() {
		return transformator;
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return indices;
	}

	@Override
	protected RootVertex<User> getRootVertex() {
		return boot.meshRoot().getUserRoot();
	}

	@Override
	public Map<String, String> getIndices() {
		return Collections.singletonMap(User.TYPE, User.TYPE);
	}
}
