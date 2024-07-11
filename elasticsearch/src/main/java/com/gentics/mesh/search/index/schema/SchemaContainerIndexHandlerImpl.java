package com.gentics.mesh.search.index.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for schema container index.
 */
@Singleton
public class SchemaContainerIndexHandlerImpl extends AbstractIndexHandler<Schema> implements SchemaIndexHandler {

	protected final SchemaTransformer transformer;

	protected final SchemaMappingProvider mappingProvider;

	@Inject
	public SchemaContainerIndexHandlerImpl(SearchProvider searchProvider, Database db, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetricsFactory, BucketManager bucketManager, SchemaTransformer transformer, SchemaMappingProvider mappingProvider) {
		super(searchProvider, db, helper, options, syncMetricsFactory, bucketManager);
		this.transformer = transformer;
		this.mappingProvider = mappingProvider;
	}

	@Override
	public String getType() {
		return "schema";
	}

	@Override
	public Class<Schema> getElementClass() {
		return Schema.class;
	}

	@Override
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.schemaDao().count();
		});
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
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return diffAndSync(Schema.composeIndexName(), null, indexPattern);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, Schema.composeIndexName());
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return Collections.singleton(Schema.composeIndexName());
	}

	@Override
	public Map<String, Optional<IndexInfo>> getIndices() {
		String indexName = Schema.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "schema");
		return Collections.singletonMap(indexName, Optional.of(info));
	}

	@Override
	public Function<String, Schema> elementLoader() {
		return (uuid) -> Tx.get().schemaDao().findByUuid(uuid);
	}

	@Override
	public Function<Collection<String>, Stream<Pair<String, Schema>>> elementsLoader() {
		return (uuids) -> Tx.get().schemaDao().findByUuids(uuids);
	}

	@Override
	public Stream<? extends Schema> loadAllElements() {
		return Tx.get().schemaDao().findAll().stream();
	}
}
