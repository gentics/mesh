package com.gentics.mesh.search.index.project;

import java.util.Collection;
import java.util.Collections;
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
import com.gentics.mesh.core.data.project.HibProject;
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
 * Handler for the project specific search index.
 */
@Singleton
public class ProjectIndexHandlerImpl extends AbstractIndexHandler<HibProject> implements ProjectIndexHandler {

	protected final ProjectTransformer transformer;

	protected final ProjectMappingProvider mappingProvider;

	@Inject
	public ProjectIndexHandlerImpl(SearchProvider searchProvider, Database db, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetricsFactory, BucketManager bucketManager, ProjectTransformer transformer, ProjectMappingProvider mappingProvider) {
		super(searchProvider, db, helper, options, syncMetricsFactory, bucketManager);
		this.transformer = transformer;
		this.mappingProvider = mappingProvider;
	}

	@Override
	public String getType() {
		return "project";
	}

	@Override
	public Class<HibProject> getElementClass() {
		return HibProject.class;
	}

	@Override
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.projectDao().count();
		});
	}

	@Override
	public ProjectTransformer getTransformer() {
		return transformer;
	}

	@Override
	public ProjectMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return diffAndSync(HibProject.composeIndexName(), null, indexPattern);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return indices.stream()
			.filter(i -> i.startsWith(getType()))
			.filter(i -> !i.equals(HibProject.composeIndexName()))
			.collect(Collectors.toSet());
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return Collections.singleton(HibProject.composeIndexName());
	}

	@Override
	public Function<String, HibProject> elementLoader() {
		return (uuid) -> Tx.get().projectDao().findByUuid(uuid);
	}

	@Override
	public Function<Collection<String>, Stream<Pair<String, HibProject>>> elementsLoader() {
		return (uuids) -> Tx.get().projectDao().findByUuids(uuids);
	}

	@Override
	public Stream<? extends HibProject> loadAllElements() {
		return Tx.get().projectDao().findAll().stream();
	}

	@Override
	public Map<String, Optional<IndexInfo>> getIndices() {
		String indexName = HibProject.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "project");
		return Collections.singletonMap(indexName, Optional.of(info));
	}

}
