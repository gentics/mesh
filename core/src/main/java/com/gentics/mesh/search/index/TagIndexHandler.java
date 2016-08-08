package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;

import io.vertx.core.json.JsonObject;

@Component
public class TagIndexHandler extends AbstractIndexHandler<Tag> {
	/**
	 * Name of the custom property of SearchQueueEntry containing the project uuid
	 */
	public final static String CUSTOM_PROJECT_UUID = "projectUuid";

	private static TagIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static TagIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return getIndexName(entry.getCustomProperty(CUSTOM_PROJECT_UUID));
	}

	@Override
	public Set<String> getIndices() {
		return db.noTx(() -> {
			ProjectRoot projectRoot = BootstrapInitializer.getBoot().meshRoot().getProjectRoot();
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

	@Override
	protected JsonObject transformToDocument(Tag tag) {
		JsonObject document = new JsonObject();
		Map<String, String> tagFields = new HashMap<>();
		tagFields.put(NAME_KEY, tag.getName());
		document.put("fields", tagFields);
		addBasicReferences(document, tag);
		addTagFamily(document, tag.getTagFamily());
		addProject(document, tag.getProject());
		return document;
	}

	private void addTagFamily(JsonObject document, TagFamily tagFamily) {
		JsonObject info = new JsonObject();
		info.put(NAME_KEY, tagFamily.getName());
		info.put(UUID_KEY, tagFamily.getUuid());
		document.put("tagFamily", info);
	}

	@Override
	protected JsonObject getMapping() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}

}
