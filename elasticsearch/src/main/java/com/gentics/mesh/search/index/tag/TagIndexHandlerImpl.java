package com.gentics.mesh.search.index.tag;

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
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.data.tag.Tag;
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
 * Handler for the tag specific search index.
 */
@Singleton
public class TagIndexHandlerImpl extends AbstractIndexHandler<Tag> implements TagIndexHandler {

	/**
	 * Name of the custom property of SearchQueueEntry containing the project uuid.
	 */
	public final static String CUSTOM_PROJECT_UUID = "projectUuid";

	protected final TagTransformer transforer;

	protected final TagMappingProvider mappingProvider;

	@Inject
	public TagIndexHandlerImpl(SearchProvider searchProvider, Database db, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetricsFactory, BucketManager bucketManager, TagTransformer transforer, TagMappingProvider mappingProvider) {
		super(searchProvider, db, helper, options, syncMetricsFactory, bucketManager);
		this.transforer = transforer;
		this.mappingProvider = mappingProvider;
	}

	@Override
	public String getType() {
		return "tag";
	}

	@Override
	public Class<Tag> getElementClass() {
		return Tag.class;
	}

	@Override
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.tagDao().count();
		});
	}

	@Override
	protected TagTransformer getTransformer() {
		return transforer;
	}

	@Override
	public TagMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Map<String, Optional<IndexInfo>> getIndices() {
		return db.tx(tx -> {
			Map<String, Optional<IndexInfo>> indexInfo = new HashMap<>();
			for (Project project : tx.projectDao().findAll()) {
				IndexInfo info = getIndex(project.getUuid());
				indexInfo.put(info.getIndexName(), Optional.of(info));
			}
			return indexInfo;
		});
	}

	/**
	 * Return the index information (settings,mapping) for the tag index of the project.
	 * 
	 * @param projectUuid
	 * @return
	 */
	public IndexInfo getIndex(String projectUuid) {
		return new IndexInfo(Tag.composeIndexName(projectUuid), null, getMappingProvider().getMapping(), "tag");
	}

	@Override
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return Flowable.defer(() -> db.tx(tx -> {
			return tx.projectDao().findAll().stream()
				.map(project -> {
					String uuid = project.getUuid();
					return diffAndSync(Tag.composeIndexName(uuid), uuid, indexPattern);
				}).collect(Collectors.collectingAndThen(Collectors.toList(), Flowable::merge));
		}));
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return db.tx(tx -> {
			Set<String> activeIndices = new HashSet<>();
			ProjectDao projectDao = tx.projectDao();
			for (Project project : projectDao.findAll()) {
				activeIndices.add(Tag.composeIndexName(project.getUuid()));
			}
			return indices.stream()
				.filter(i -> i.startsWith(getType() + "-"))
				.filter(i -> !activeIndices.contains(i))
				.collect(Collectors.toSet());
		});
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return db.tx(tx -> {
			Project project = tx.getProject(ac);
			if (project != null) {
				return Collections.singleton(Tag.composeIndexName(project.getUuid()));
			} else {
				return getIndices().keySet();
			}
		});
	}

	@Override
	public Function<String, Tag> elementLoader() {
		return uuid -> Tx.get().tagDao().findByUuid(uuid);
	}

	@Override
	public Function<Collection<String>, Stream<Pair<String, Tag>>> elementsLoader() {
		return (uuids) -> Tx.get().tagDao().findByUuids(uuids);
	}

	@Override
	public Stream<? extends Tag> loadAllElements() {
		return Tx.get().tagDao().findAll().stream();
	}

	@Override
	public boolean isDefinitionDataDependent() {
		// We depend on the project.
		return true;
	}
}
