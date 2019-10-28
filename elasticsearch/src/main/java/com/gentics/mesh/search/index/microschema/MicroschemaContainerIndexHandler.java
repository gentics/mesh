package com.gentics.mesh.search.index.microschema;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for the elastic search microschema index.
 */
@Singleton
public class MicroschemaContainerIndexHandler extends AbstractIndexHandler<MicroschemaContainer> {

	@Inject
	MicroschemaTransformer transformer;

	@Inject
	MicroschemaMappingProvider mappingProvider;

	@Inject
	SyncMetersFactory syncMetersFactory;

	@Inject
	public MicroschemaContainerIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper, MeshOptions options, SyncMetersFactory syncMetricsFactory) {
		super(searchProvider, db, boot, helper, options, syncMetricsFactory);
	}

	@Override
	public String getType() {
		return "microschema";
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return MicroschemaContainer.composeIndexName();
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return MicroschemaContainer.composeDocumentId(entry.getElementUuid());
	}

	@Override
	public Class<?> getElementClass() {
		return MicroschemaContainer.class;
	}

	@Override
	public MicroschemaTransformer getTransformer() {
		return transformer;
	}

	@Override
	public MicroschemaMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Flowable<SearchRequest> syncIndices() {
		return diffAndSync(MicroschemaContainer.composeIndexName(), null);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, MicroschemaContainer.composeIndexName());
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(MicroschemaContainer.composeIndexName());
	}

	@Override
	public RootVertex<MicroschemaContainer> getRootVertex() {
		return boot.meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String indexName = MicroschemaContainer.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "microschema");
		return Collections.singletonMap(indexName, info);
	}

}
