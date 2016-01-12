package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.RootVertex;

import io.vertx.core.json.JsonObject;

@Component
public class TagFamilyIndexHandler extends AbstractIndexHandler<TagFamily> {

	private static TagFamilyIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static TagFamilyIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex() {
		return "tag_family";
	}

	@Override
	protected String getType() {
		return "tagFamily";
	}

	@Override
	protected RootVertex<TagFamily> getRootVertex() {
		return boot.meshRoot().getTagFamilyRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(TagFamily tagFamily) {
		Map<String, Object> map = new HashMap<>();
		map.put(NAME_KEY, tagFamily.getName());
		addBasicReferences(map, tagFamily);
		addTags(map, tagFamily.getTagRoot().findAll());
		addProject(map, tagFamily.getProject());
		return map;
	}

	@Override
	protected JsonObject getMapping() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}

}
