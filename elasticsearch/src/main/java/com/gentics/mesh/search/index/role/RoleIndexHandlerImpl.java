package com.gentics.mesh.search.index.role;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for the elasticsearch role index.
 */
@Singleton
public class RoleIndexHandlerImpl extends AbstractIndexHandler<HibRole>  implements RoleIndexHandler {

	@Inject
	RoleTransformer transformer;

	@Inject
	RoleMappingProvider mappingProvider;

	@Inject
	public RoleIndexHandlerImpl(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetricsFactory, BucketManager bucketManager) {
		super(searchProvider, db, boot, helper, options, syncMetricsFactory, bucketManager);
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
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.roleDao().globalCount();
		});
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
	public MappingProvider getMappingProvider() {
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
		return diffAndSync(Role.composeIndexName(), null);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, Role.composeIndexName());
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return Collections.singleton(Role.composeIndexName());
	}

	@Override
	public Function<String, HibRole> elementLoader() {
		return (uuid) -> boot.meshRoot().getRoleRoot().findByUuid(uuid);
	}

	@Override
	public Stream<? extends HibRole> loadAllElements() {
		return Tx.get().roleDao().findAll().stream();
	}

}
