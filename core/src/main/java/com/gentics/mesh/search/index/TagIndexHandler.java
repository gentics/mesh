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

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.RootVertex;

import io.vertx.core.json.JsonObject;

@Component
public class TagIndexHandler extends AbstractIndexHandler<Tag> {

	private static TagIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static TagIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex() {
		return Tag.TYPE;
	}

	@Override
	protected String getType() {
		return Tag.TYPE;
	}

	@Override
	protected RootVertex<Tag> getRootVertex() {
		return boot.meshRoot().getTagRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(Tag tag) {
		Map<String, Object> map = new HashMap<>();
		Map<String, String> tagFields = new HashMap<>();
		tagFields.put(NAME_KEY, tag.getName());
		map.put("fields", tagFields);
		addBasicReferences(map, tag);
		addTagFamily(map, tag.getTagFamily());
		addProject(map, tag.getProject());
		return map;
	}

	private void addTagFamily(Map<String, Object> map, TagFamily tagFamily) {
		Map<String, Object> tagFamilyFields = new HashMap<>();
		tagFamilyFields.put(NAME_KEY, tagFamily.getName());
		tagFamilyFields.put(UUID_KEY, tagFamily.getUuid());
		map.put("tagFamily", tagFamilyFields);
	}

	@Override
	protected JsonObject getMapping() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}

}
