package com.gentics.mesh.search.index.user;

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
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.data.user.HibUser;
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
 * @see UserIndexHandler
 */
@Singleton
public class UserIndexHandlerImpl extends AbstractIndexHandler<HibUser> implements UserIndexHandler {

	private final static Set<String> indices = Collections.singleton(HibUser.composeIndexName());

	protected final UserTransformer transformer;

	protected final UserMappingProvider mappingProvider;

	@Inject
	public UserIndexHandlerImpl(SearchProvider searchProvider, Database db, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetricsFactory, BucketManager bucketManager, UserTransformer transformer, UserMappingProvider mappingProvider) {
		super(searchProvider, db, helper, options, syncMetricsFactory, bucketManager);
		this.transformer = transformer;
		this.mappingProvider = mappingProvider;
	}

	@Override
	public String getType() {
		return "user";
	}

	@Override
	public Class<HibUser> getElementClass() {
		return HibUser.class;
	}

	@Override
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.userDao().count();
		});
	}

	@Override
	public UserTransformer getTransformer() {
		return transformer;
	}

	@Override
	public MappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return diffAndSync(HibUser.composeIndexName(), null, indexPattern);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, HibUser.composeIndexName());
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return indices;
	}

	@Override
	public Function<String, HibUser> elementLoader() {
		return uuid -> Tx.get().userDao().findByUuid(uuid);
	}

	@Override
	public Function<Collection<String>, Stream<Pair<String, HibUser>>> elementsLoader() {
		return (uuids) -> Tx.get().userDao().findByUuids(uuids);
	}

	@Override
	public Stream<? extends HibUser> loadAllElements() {
		return Tx.get().userDao().findAll().stream();
	}

	@Override
	public Map<String, Optional<IndexInfo>> getIndices() {
		String indexName = HibUser.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "user");
		return Collections.singletonMap(indexName, Optional.of(info));
	}
}
