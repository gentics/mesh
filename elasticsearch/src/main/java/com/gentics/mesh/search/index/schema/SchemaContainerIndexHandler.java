package com.gentics.mesh.search.index.schema;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetric;

import io.reactivex.Completable;

/**
 * Handler for schema container index.
 */
@Singleton
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

	@Inject
	SchemaTransformer transformer;

	@Inject
	SchemaMappingProvider mappingProvider;

	@Inject
	public SchemaContainerIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot) {
		super(searchProvider, db, boot);
	}

	@Override
	public String getType() {
		return "schema";
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return SchemaContainer.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return SchemaContainer.composeIndexName();
	}

	@Override
	public Class<SchemaContainer> getElementClass() {
		return SchemaContainer.class;
	}

	@Override
	public SchemaTransformer getTransformer() {
		return transformer;
	}

	@Override
	public MappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Completable syncIndices() {
		return Completable.defer(() -> {
			return diffAndSync(SchemaContainer.composeIndexName(), null, new SyncMetric(getType()));
		});
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, SchemaContainer.composeIndexName());
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(SchemaContainer.composeIndexName());
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String indexName = SchemaContainer.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "schema");
		return Collections.singletonMap(indexName, info);
	}

	@Override
	public RootVertex<SchemaContainer> getRootVertex() {
		return boot.meshRoot().getSchemaContainerRoot();
	}

}
