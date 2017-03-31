package com.gentics.mesh.search.index.tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

import rx.Completable;

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
	TagTransformator transformator;

	@Inject
	public TagIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	protected Class<Tag> getElementClass() {
		return Tag.class;
	}

	@Override
	protected TagTransformator getTransformator() {
		return transformator;
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
	protected String composeIndexTypeFromEntry(UpdateDocumentEntry entry) {
		return Tag.composeTypeName();
	}

	@Override
	public Completable store(Tag tag, UpdateDocumentEntry entry) {
		entry.getContext().setProjectUuid(tag.getProject().getUuid());
		return super.store(tag, entry);
	}

	@Override
	public Map<String, String> getIndices() {
		return db.noTx(() -> {
			Map<String, String> indexInfo = new HashMap<>();
			ProjectRoot projectRoot = boot.meshRoot().getProjectRoot();
			projectRoot.reload();
			List<? extends Project> projects = projectRoot.findAll();
			for (Project project : projects) {
				indexInfo.put(Tag.composeIndexName(project.getUuid()), Tag.TYPE);
			}
			return indexInfo;
		});
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return db.noTx(() -> {
			Project project = ac.getProject();
			if (project != null) {
				return Collections.singleton(Tag.composeIndexName(project.getUuid()));
			} else {
				return getIndices().keySet();
			}
		});
	}

	@Override
	protected RootVertex<Tag> getRootVertex() {
		return boot.meshRoot().getTagRoot();
	}

}
