package com.gentics.mesh.search.index.tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.db.Database;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.bulk.IndexBulkEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetric;

import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * Handler for the tag specific search index.
 */
@Singleton
public class TagIndexHandler extends AbstractIndexHandler<Tag> {

	/**
	 * Name of the custom property of SearchQueueEntry containing the project uuid.
	 */
	public final static String CUSTOM_PROJECT_UUID = "projectUuid";

	@Inject
	TagTransformer transforer;

	@Inject
	TagMappingProvider mappingProvider;

	@Inject
	public TagIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
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
	protected TagTransformer getTransformer() {
		return transforer;
	}

	@Override
	public TagMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return Tag.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return Tag.composeIndexName(entry.getContext().getProjectUuid());
	}

	@Override
	public Completable store(Tag tag, UpdateDocumentEntry entry) {
		entry.getContext().setProjectUuid(tag.getProject().getUuid());
		return super.store(tag, entry);
	}

	@Override
	public Observable<IndexBulkEntry> storeForBulk(Tag tag, UpdateDocumentEntry entry) {
		entry.getContext().setProjectUuid(tag.getProject().getUuid());
		return super.storeForBulk(tag, entry);
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		return db.tx(() -> {
			Map<String, IndexInfo> indexInfo = new HashMap<>();
			ProjectRoot projectRoot = boot.meshRoot().getProjectRoot();
			for (Project project : projectRoot.findAll()) {
				String indexName = Tag.composeIndexName(project.getUuid());
				IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "tag");
				indexInfo.put(indexName, info);
			}
			return indexInfo;
		});
	}

	@Override
	public Completable syncIndices() {
		return Completable.defer(() -> {
			return db.tx(() -> {
				ProjectRoot root = boot.meshRoot().getProjectRoot();
				Set<Completable> actions = new HashSet<>();
				SyncMetric metric = new SyncMetric(getType());
				for (Project project : root.findAll()) {
					String uuid = project.getUuid();
					actions.add(diffAndSync(Tag.composeIndexName(uuid), uuid, metric));
				}

				return Completable.merge(actions);
			});
		});
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return db.tx(() -> {
			Set<String> activeIndices = new HashSet<>();
			for (Project project : boot.meshRoot().getProjectRoot().findAll()) {
				activeIndices.add(Tag.composeIndexName(project.getUuid()));
			}
			return indices.stream()
				.filter(i -> i.startsWith(getType() + "-"))
				.filter(i -> !activeIndices.contains(i))
				.collect(Collectors.toSet());
		});
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return db.tx(() -> {
			Project project = ac.getProject();
			if (project != null) {
				return Collections.singleton(Tag.composeIndexName(project.getUuid()));
			} else {
				return getIndices().keySet();
			}
		});
	}

	@Override
	public RootVertex<Tag> getRootVertex() {
		return boot.meshRoot().getTagRoot();
	}

}
