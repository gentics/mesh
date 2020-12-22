package com.gentics.mesh.search.index.project;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
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

	@Inject
	ProjectTransformer transformer;

	@Inject
	ProjectMappingProvider mappingProvider;

	@Inject
	public ProjectIndexHandlerImpl(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper, AbstractMeshOptions options,
		SyncMetersFactory syncMetricsFactory, BucketManager bucketManager) {
		super(searchProvider, db, boot, helper, options, syncMetricsFactory, bucketManager);
	}

	@Override
	public String getType() {
		return "project";
	}

	@Override
	public Class<Project> getElementClass() {
		return Project.class;
	}

	@Override
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.projectDao().globalCount();
		});
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return Project.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return Project.composeIndexName();
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
	public Flowable<SearchRequest> syncIndices() {
		return diffAndSync(Project.composeIndexName(), null);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return indices.stream()
			.filter(i -> i.startsWith(getType()))
			.filter(i -> !i.equals(Project.composeIndexName()))
			.collect(Collectors.toSet());
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return Collections.singleton(Project.composeIndexName());
	}

	@Override
	public Function<String, HibProject> elementLoader() {
		return (uuid) -> boot.meshRoot().getProjectRoot().findByUuid(uuid);
	}

	@Override
	public Stream<? extends HibProject> loadAllElements() {
		return Tx.get().projectDao().findAll().stream();
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String indexName = Project.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "project");
		return Collections.singletonMap(indexName, info);
	}

}
