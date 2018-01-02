package com.gentics.mesh.search.index.tagfamily;

import java.util.Collections;
import java.util.HashMap;
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
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

import io.reactivex.Completable;

@Singleton
public class TagFamilyIndexHandler extends AbstractIndexHandler<TagFamily> {

	@Inject
	TagFamilyTransformer transformer;

	@Inject
	TagFamilyMappingProvider mappingProvider;

	@Inject
	public TagFamilyIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	public Class<TagFamily> getElementClass() {
		return TagFamily.class;
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
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return TagFamily.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return TagFamily.composeIndexName(entry.getContext().getProjectUuid());
	}

	@Override
	public Completable store(TagFamily tagFamily, UpdateDocumentEntry entry) {
		entry.getContext().setProjectUuid(tagFamily.getProject().getUuid());
		return super.store(tagFamily, entry);
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		return db.tx(() -> {
			ProjectRoot root = boot.meshRoot().getProjectRoot();
			Map<String, IndexInfo> indexInfo = new HashMap<>();
			for (Project project : root.findAllIt()) {
				String indexName = TagFamily.composeIndexName(project.getUuid());
				IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping());
				indexInfo.put(indexName, info);
			}
			return indexInfo;
		});
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return db.tx(() -> {
			Project project = ac.getProject();
			if (project != null) {
				return Collections.singleton(TagFamily.composeIndexName(project.getUuid()));
			} else {
				return getIndices().keySet();
			}
		});
	}

	@Override
	public RootVertex<TagFamily> getRootVertex() {
		return boot.meshRoot().getTagFamilyRoot();
	}

}
