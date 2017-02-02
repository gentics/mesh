package com.gentics.mesh.search.index.tagfamily;

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
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

import rx.Completable;

@Singleton
public class TagFamilyIndexHandler extends AbstractIndexHandler<TagFamily> {

	@Inject
	TagFamilyTransformator transformator;

	@Inject
	public TagFamilyIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	protected Class<TagFamily> getElementClass() {
		return TagFamily.class;
	}

	public TagFamilyTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return TagFamily.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return TagFamily.composeIndexName(entry.getContext().getProjectUuid());
	}

	@Override
	protected String composeIndexTypeFromEntry(UpdateDocumentEntry entry) {
		return TagFamily.composeTypeName();
	}

	@Override
	public Completable store(TagFamily tagFamily, UpdateDocumentEntry entry) {
		entry.getContext().setProjectUuid(tagFamily.getProject().getUuid());
		return super.store(tagFamily, entry);
	}

	@Override
	public Map<String, String> getIndices() {
		return db.noTx(() -> {
			ProjectRoot root = boot.meshRoot().getProjectRoot();
			root.reload();
			List<? extends Project> projects = root.findAll();
			Map<String, String> indexInfo = new HashMap<>();
			for (Project project : projects) {
				indexInfo.put(TagFamily.composeIndexName(project.getUuid()), TagFamily.TYPE);
			}
			return indexInfo;
		});
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return db.noTx(() -> {
			Project project = ac.getProject();
			if (project != null) {
				return Collections.singleton(TagFamily.composeIndexName(project.getUuid()));
			} else {
				return getIndices().keySet();
			}
		});
	}

	@Override
	protected RootVertex<TagFamily> getRootVertex() {
		return boot.meshRoot().getTagFamilyRoot();
	}

}
