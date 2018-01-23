package com.gentics.mesh.search.index.project;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

/**
 * Handler for the project specific search index.
 */
@Singleton
public class ProjectIndexHandler extends AbstractIndexHandler<Project> {

	@Inject
	ProjectTransformer transformer;

	@Inject
	ProjectMappingProvider mappingProvider;

	@Inject
	public ProjectIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	public Class<Project> getElementClass() {
		return Project.class;
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
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(Project.composeIndexName());
	}

	@Override
	public RootVertex<Project> getRootVertex() {
		return boot.meshRoot().getProjectRoot();
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String indexName = Project.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping());
		return Collections.singletonMap(indexName, info);
	}

}
