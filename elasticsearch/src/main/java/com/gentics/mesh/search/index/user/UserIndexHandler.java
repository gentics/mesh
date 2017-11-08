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
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

@Singleton
public class UserIndexHandler extends AbstractIndexHandler<User> {

	private final static Set<String> indices = Collections.singleton(User.TYPE);

	@Inject
	UserTransformer transformer;

	@Inject
	UserMappingProvider mappingProvider;

	@Inject
	public UserIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	public Class<User> getElementClass() {
		return User.class;
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return User.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return User.composeIndexName();
	}

	@Override
	protected String composeIndexTypeFromEntry(UpdateDocumentEntry entry) {
		return User.composeIndexType();
	}

	@Override
	public UserTransformer getTransformer() {
		return transformer;
	}

	@Override
	protected MappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return indices;
	}

	@Override
	public RootVertex<User> getRootVertex() {
		return boot.meshRoot().getUserRoot();
	}

	@Override
	public Map<String, String> getIndices() {
		return Collections.singletonMap(User.TYPE, User.TYPE);
	}
}
