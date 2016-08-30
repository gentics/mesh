package com.gentics.mesh.search.index.tag;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.dagger.MeshCore;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

public class TagIndexHandler extends AbstractIndexHandler<Tag> {

	/**
	 * Name of the custom property of SearchQueueEntry containing the project uuid
	 */
	public final static String CUSTOM_PROJECT_UUID = "projectUuid";

	private TagTransformator transformator = new TagTransformator();

	@Inject
	public TagIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot) {
		super(searchProvider, db, boot);
	}

	@Override
	protected TagTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return getIndexName(entry.getCustomProperty(CUSTOM_PROJECT_UUID));
	}

	@Override
	public Set<String> getIndices() {
		return db.noTx(() -> {
			ProjectRoot projectRoot = boot.meshRoot().getProjectRoot();
			projectRoot.reload();
			List<? extends Project> projects = projectRoot.findAll();
			Set<String> indices = projects.stream().map(project -> getIndexName(project.getUuid())).collect(Collectors.toSet());
			return indices;
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
		StringBuilder indexName = new StringBuilder(Tag.TYPE);
		indexName.append("-").append(projectUuid);
		return indexName.toString();
	}

	@Override
	protected String getType() {
		return Tag.TYPE;
	}

	@Override
	public String getKey() {
		return Tag.TYPE;
	}

	@Override
	protected RootVertex<Tag> getRootVertex() {
		return boot.meshRoot().getTagRoot();
	}

}
