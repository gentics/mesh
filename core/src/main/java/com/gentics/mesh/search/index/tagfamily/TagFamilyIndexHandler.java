package com.gentics.mesh.search.index.tagfamily;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.dagger.MeshInternal;
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
		// The document type for tag families is not specific.
		return getDocumentType();
	}

	private String getDocumentType() {
		return TagFamily.TYPE;
	}

	@Override
	public Map<String, Set<String>> getIndices() {
		return db.noTx(() -> {
			ProjectRoot root = boot.meshRoot().getProjectRoot();
			root.reload();
			List<? extends Project> projects = root.findAll();
			Map<String, Set<String>> indexInfo = new HashMap<>();
			for (Project project : projects) {
				indexInfo.put(getIndexName(project.getUuid()), Collections.singleton(getDocumentType()));
			}
			return indexInfo;
		});
	}

	@Override
	public Set<String> getAffectedIndices(InternalActionContext ac) {
		return db.noTx(() -> {
			Project project = ac.getProject();
			if (project != null) {
				return Collections.singleton(getIndexName(project.getUuid()));
			} else {
				return getIndices().keySet();
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
		return MeshInternal.get().boot().meshRoot().getTagFamilyRoot();
	}

}
