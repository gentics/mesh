package com.gentics.mesh.search.index.schema;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

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
	public SchemaContainerIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetricsFactory) {
		super(searchProvider, db, boot, helper, options, syncMetricsFactory);
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
	public Flowable<SearchRequest> syncIndices() {
		return diffAndSync(SchemaContainer.composeIndexName(), null);
	}

	@Override
	public Transactional<Set<String>> filterUnknownIndices(Set<String> indices) {
		return Transactional.of(filterIndicesByType(indices, SchemaContainer.composeIndexName()));
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(SchemaContainer.composeIndexName());
	}

	@Override
	public Transactional<Map<String, IndexInfo>> getIndices() {
		String indexName = SchemaContainer.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "schema");
		return Transactional.of(Collections.singletonMap(indexName, info));
	}

	@Override
	public Function<String, SchemaContainer> elementLoader() {
		return (uuid) -> boot.meshRoot().getSchemaContainerRoot().findByUuid(uuid);
	}

	@Override
	public Stream<? extends SchemaContainer> loadAllElements() {
		return boot.meshRoot().getSchemaContainerRoot().findAll().stream();
	}
}
