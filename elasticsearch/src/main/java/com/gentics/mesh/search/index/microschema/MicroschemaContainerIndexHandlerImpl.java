package com.gentics.mesh.search.index.microschema;

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
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for the elastic search microschema index.
 */
@Singleton
public class MicroschemaContainerIndexHandlerImpl extends AbstractIndexHandler<HibMicroschema> implements MicroschemaIndexHandler {

	protected final MicroschemaTransformer transformer;

	protected final MicroschemaMappingProvider mappingProvider;

	protected final SyncMetersFactory syncMetersFactory;

	@Inject
	public MicroschemaContainerIndexHandlerImpl(SearchProvider searchProvider, Database db, MeshHelper helper,
		MeshOptions options, SyncMetersFactory syncMetricsFactory, BucketManager bucketManager, MicroschemaTransformer transformer, MicroschemaMappingProvider mappingProvider, SyncMetersFactory syncMetersFactory) {
		super(searchProvider, db, helper, options, syncMetricsFactory, bucketManager);
		this.transformer = transformer;
		this.mappingProvider = mappingProvider;
		this.syncMetersFactory = syncMetersFactory;
	}

	@Override
	public String getType() {
		return "microschema";
	}

	@Override
	public Class<? extends HibBucketableElement> getElementClass() {
		return HibMicroschema.class;
	}

	@Override
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.microschemaDao().count();
		});
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
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return diffAndSync(HibMicroschema.composeIndexName(), null, indexPattern);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, HibMicroschema.composeIndexName());
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return Collections.singleton(HibMicroschema.composeIndexName());
	}

	@Override
	public Function<String, HibMicroschema> elementLoader() {
		return (uuid) -> Tx.get().microschemaDao().findByUuid(uuid);
	}

	@Override
	public Function<Collection<String>, Stream<Pair<String, HibMicroschema>>> elementsLoader() {
		return (uuids) -> Tx.get().microschemaDao().findByUuids(uuids);
	}

	@Override
	public Stream<? extends HibMicroschema> loadAllElements() {
		return Tx.get().microschemaDao().findAll().stream();
	}

	@Override
	public Map<String, Optional<IndexInfo>> getIndices() {
		String indexName = HibMicroschema.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "microschema");
		return Collections.singletonMap(indexName, Optional.of(info));
	}

}
