package com.gentics.mesh.search.index.role;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetric;

import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;
import io.reactivex.Flowable;

/**
 * Handler for the elasticsearch role index.
 */
@Singleton
public class RoleIndexHandler extends AbstractIndexHandler<Role> {

	@Inject
	RoleTransformer transformer;

	@Inject
	RoleMappingProvider mappingProvider;

	@Inject
	public RoleIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper) {
		super(searchProvider, db, boot, helper);
	}

	@Override
	public String getType() {
		return "role";
	}

	@Override
	public Class<Role> getElementClass() {
		return Role.class;
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return Role.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return Role.composeIndexName();
	}

	@Override
	public RoleTransformer getTransformer() {
		return transformer;
	}

	@Override
	protected MappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String indexName = Role.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "role");
		return Collections.singletonMap(indexName, info);
	}

	@Override
	public Flowable<SearchRequest> syncIndices() {
		return diffAndSync(Role.composeIndexName(), null, new SyncMetric(getType()));
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, Role.composeIndexName());
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(Role.composeIndexName());
	}

	@Override
	public RootVertex<Role> getRootVertex() {
		return boot.meshRoot().getRoleRoot();
	}

}
