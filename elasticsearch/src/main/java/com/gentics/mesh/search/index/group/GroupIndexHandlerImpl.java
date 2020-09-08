package com.gentics.mesh.search.index.group;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for the elastic search group index.
 */
@Singleton
public class GroupIndexHandlerImpl extends AbstractIndexHandler<HibGroup> implements GroupIndexHandler {

	@Inject
	GroupTransformer transformer;

	@Inject
	GroupMappingProvider mappingProvider;

	@Inject
	public GroupIndexHandlerImpl(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetersFactory) {
		super(searchProvider, db, boot, helper, options, syncMetersFactory);
	}

	@Override
	public String getType() {
		return "group";
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
	public GroupMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String indexName = Group.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "group");
		return Collections.singletonMap(indexName, info);
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
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return Collections.singleton(Group.composeIndexName());
	}

	@Override
	public Function<String, HibGroup> elementLoader() {
		return (uuid) -> boot.meshRoot().getGroupRoot().findByUuid(uuid);
	}

	@Override
	public Stream<? extends HibGroup> loadAllElements(Tx tx) {
		return tx.groupDao().findAll().stream();
	}

	@Override
	public Flowable<SearchRequest> syncIndices() {
		return diffAndSync(Group.composeIndexName(), null);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, Group.composeIndexName());
	}

}
