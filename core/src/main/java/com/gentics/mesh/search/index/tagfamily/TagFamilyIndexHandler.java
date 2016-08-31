package com.gentics.mesh.search.index.tagfamily;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.dagger.MeshCore;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

public class TagFamilyIndexHandler extends AbstractIndexHandler<TagFamily> {

	/**
	 * Name of the custom property of SearchQueueEntry containing the project UUID.
	 */
	public final static String CUSTOM_PROJECT_UUID = "projectUuid";

	private TagFamilyTransformator transformator = new TagFamilyTransformator();

	@Inject
	public TagFamilyIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot) {
		super(searchProvider, db, boot);
	}

	public TagFamilyTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return getIndexName(entry.get(CUSTOM_PROJECT_UUID));
	}

	@Override
	protected String getDocumentType(SearchQueueEntry entry) {
		return TagFamily.TYPE;
	}

	@Override
	public Set<String> getIndices() {
		return db.noTx(() -> {
			ProjectRoot root = boot.meshRoot().getProjectRoot();
			root.reload();
			List<? extends Project> projects = root.findAll();
			return projects.stream().map(project -> getIndexName(project.getUuid())).collect(Collectors.toSet());
		});
	}

	@Override
	public Set<String> getAffectedIndices(InternalActionContext ac) {
		return db.noTx(() -> {
			Project project = ac.getProject();
			if (project != null) {
				return Collections.singleton(getIndexName(project.getUuid()));
			} else {
				return getIndices();
			}
		});
	}

	/**
	 * Get the index name for the given project.
	 * 
	 * @param project
	 *            Uuid
	 * @return index name
	 */
	public static String getIndexName(String projectUuid) {
		StringBuilder indexName = new StringBuilder("tag-family");
		indexName.append("-").append(projectUuid);
		return indexName.toString();
	}

	@Override
	public String getKey() {
		return TagFamily.TYPE;
	}

	@Override
	protected RootVertex<TagFamily> getRootVertex() {
		return MeshCore.get().boot().meshRoot().getTagFamilyRoot();
	}

}
