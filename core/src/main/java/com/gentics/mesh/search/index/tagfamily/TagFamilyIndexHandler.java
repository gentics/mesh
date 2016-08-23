package com.gentics.mesh.search.index.tagfamily;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.search.index.AbstractIndexHandler;

@Component
public class TagFamilyIndexHandler extends AbstractIndexHandler<TagFamily> {

	/**
	 * Name of the custom property of SearchQueueEntry containing the project UUID.
	 */
	public final static String CUSTOM_PROJECT_UUID = "projectUuid";

	private static TagFamilyIndexHandler instance;

	private TagFamilyTransformator transformator = new TagFamilyTransformator();

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static TagFamilyIndexHandler getInstance() {
		return instance;
	}

	public TagFamilyTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return getIndexName(entry.getCustomProperty(CUSTOM_PROJECT_UUID));
	}

	@Override
	public Set<String> getIndices() {
		return db.noTx(() -> {
			ProjectRoot root = BootstrapInitializer.getBoot().meshRoot().getProjectRoot();
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
	protected String getType() {
		return "tagFamily";
	}

	@Override
	public String getKey() {
		return TagFamily.TYPE;
	}

	@Override
	protected RootVertex<TagFamily> getRootVertex() {
		return boot.meshRoot().getTagFamilyRoot();
	}

}
