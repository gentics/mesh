package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;

import io.vertx.core.json.JsonObject;

@Component
public class ProjectIndexHandler extends AbstractIndexHandler<Project> {

	private static ProjectIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static ProjectIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex() {
		return Project.TYPE;
	}

	@Override
	protected String getType() {
		return Project.TYPE;
	}

	@Override
	protected RootVertex<Project> getRootVertex() {
		return boot.meshRoot().getProjectRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(Project project) {
		Map<String, Object> map = new HashMap<>();
		map.put(NAME_KEY, project.getName());
		addBasicReferences(map, project);
		return map;
	}

	@Override
	protected JsonObject getMapping() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}

}
