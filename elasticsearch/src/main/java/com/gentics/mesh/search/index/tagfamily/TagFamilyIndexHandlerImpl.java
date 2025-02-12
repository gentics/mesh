package com.gentics.mesh.search.index.tagfamily;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
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
 * @see TagFamilyIndexHandler
 */
@Singleton
public class TagFamilyIndexHandlerImpl extends AbstractIndexHandler<HibTagFamily> implements TagFamilyIndexHandler {

	protected final TagFamilyTransformer transformer;

	protected final TagFamilyMappingProvider mappingProvider;

	@Inject
	public TagFamilyIndexHandlerImpl(SearchProvider searchProvider, Database db, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetricsFactory, BucketManager bucketManager, TagFamilyTransformer transformer, TagFamilyMappingProvider mappingProvider) {
		super(searchProvider, db, helper, options, syncMetricsFactory, bucketManager);
		this.transformer = transformer;
		this.mappingProvider = mappingProvider;
	}

	@Override
	public String getType() {
		return "tagfamily";
	}

	@Override
	public Class<HibTagFamily> getElementClass() {
		return HibTagFamily.class;
	}

	@Override
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.tagFamilyDao().count();
		});
	}

	@Override
	public TagFamilyTransformer getTransformer() {
		return transformer;
	}

	@Override
	public TagFamilyMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Map<String, Optional<IndexInfo>> getIndices() {
		return db.tx(tx -> {
			Map<String, Optional<IndexInfo>> indexInfo = new HashMap<>();
			for (HibProject project : tx.projectDao().findAll()) {
				String indexName = HibTagFamily.composeIndexName(project.getUuid());
				IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "tagFamily");
				indexInfo.put(indexName, Optional.of(info));
			}
			return indexInfo;
		});
	}

	@Override
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return Flowable.defer(() -> db.tx(tx -> {
			return tx.projectDao().findAll().stream()
				.map(project -> {
					String uuid = project.getUuid();
					String indexName = HibTagFamily.composeIndexName(uuid);
					return diffAndSync(indexName, uuid, indexPattern);
				}).collect(Collectors.collectingAndThen(Collectors.toList(), Flowable::merge));
		}));
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return db.tx(tx -> {
			ProjectDao projectDao = tx.projectDao();
			Set<String> activeIndices = new HashSet<>();
			for (HibProject project : projectDao.findAll()) {
				activeIndices.add(HibTagFamily.composeIndexName(project.getUuid()));
			}

			return indices.stream()
				.filter(i -> i.startsWith(getType()))
				.filter(i -> !activeIndices.contains(i))
				.collect(Collectors.toSet());
		});
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return db.tx(tx -> {
			HibProject project = tx.getProject(ac);
			if (project != null) {
				return Collections.singleton(HibTagFamily.composeIndexName(project.getUuid()));
			} else {
				return getIndices().keySet();
			}
		});
	}

	@Override
	public Function<String, HibTagFamily> elementLoader() {
		return uuid -> Tx.get().tagFamilyDao().findByUuid(uuid);
	}

	@Override
	public Function<Collection<String>, Stream<Pair<String, HibTagFamily>>> elementsLoader() {
		return (uuids) -> Tx.get().tagFamilyDao().findByUuids(uuids);
	}

	@Override
	public Stream<? extends HibTagFamily> loadAllElements() {
		return Tx.get().tagFamilyDao().findAll().stream();
	}

	@Override
	public boolean isDefinitionDataDependent() {
		// We depend on the project.
		return true;
	}
}
