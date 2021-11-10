package com.gentics.mesh.search.index.tag;

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

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.data.tag.HibTag;
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
public class TagIndexHandlerImpl extends AbstractIndexHandler<HibTag> implements TagIndexHandler {

	/**
	 * Name of the custom property of SearchQueueEntry containing the project uuid.
	 */
	public final static String CUSTOM_PROJECT_UUID = "projectUuid";

	@Inject
	TagTransformer transforer;

	@Inject
	TagMappingProvider mappingProvider;

	@Inject
	public TagIndexHandlerImpl(SearchProvider searchProvider, Database db, BootstrapInitializer boot, MeshHelper helper, MeshOptions options,
		SyncMetersFactory syncMetricsFactory, BucketManager bucketManager) {
		super(searchProvider, db, boot, helper, options, syncMetricsFactory, bucketManager);
	}

	@Override
	public String getType() {
		return "tag";
	}

	@Override
	public Class<HibTag> getElementClass() {
		return HibTag.class;
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
	public Map<String, IndexInfo> getIndices() {
		return db.tx(() -> {
			Map<String, IndexInfo> indexInfo = new HashMap<>();
			for (HibProject project : boot.projectDao().findAll()) {
				IndexInfo info = getIndex(project.getUuid());
				indexInfo.put(info.getIndexName(), info);
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
		return new IndexInfo(HibTag.composeIndexName(projectUuid), null, getMappingProvider().getMapping(), "tag");
	}

	@Override
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return Flowable.defer(() -> db.tx(() -> {
			return boot.projectDao().findAll().stream()
				.map(project -> {
					String uuid = project.getUuid();
					return diffAndSync(HibTag.composeIndexName(uuid), uuid, indexPattern);
				}).collect(Collectors.collectingAndThen(Collectors.toList(), Flowable::merge));
		}));
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return db.tx(tx -> {
			Set<String> activeIndices = new HashSet<>();
			ProjectDao projectDao = tx.projectDao();
			for (HibProject project : projectDao.findAll()) {
				activeIndices.add(HibTag.composeIndexName(project.getUuid()));
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
			HibProject project = tx.getProject(ac);
			if (project != null) {
				return Collections.singleton(HibTag.composeIndexName(project.getUuid()));
			} else {
				return getIndices().keySet();
			}
		});
	}

	@Override
	public Function<String, HibTag> elementLoader() {
		return uuid -> boot.tagDao().findByUuid(uuid);
	}

	@Override
	public Stream<? extends HibTag> loadAllElements() {
		return Tx.get().tagDao().findAll().stream();
	}

}
