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
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetric;

import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;
import io.reactivex.Flowable;

@Singleton
public class UserIndexHandler extends AbstractIndexHandler<User> {

	private final static Set<String> indices = Collections.singleton(User.composeIndexName());

	@Inject
	UserTransformer transformer;

	@Inject
	UserMappingProvider mappingProvider;

	@Inject
	public UserIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper, MeshOptions options) {
		super(searchProvider, db, boot, helper, options);
	}

	@Override
	public String getType() {
		return "user";
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
	public UserTransformer getTransformer() {
		return transformer;
	}

	@Override
	protected MappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Flowable<SearchRequest> syncIndices() {
		return diffAndSync(User.composeIndexName(), null, new SyncMetric(getType()));
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, User.composeIndexName());
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
	public Map<String, IndexInfo> getIndices() {
		String indexName = User.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "user");
		return Collections.singletonMap(indexName, info);
	}
}
