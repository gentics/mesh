package com.gentics.mesh.search.index.tag;

import static com.gentics.mesh.search.index.MappingHelper.LONG;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

/**
 * Transformator for tag search index documents.
 */
@Singleton
public class TagTransformator extends AbstractTransformator<Tag> {

	@Inject
	public TagTransformator() {
	}

	@Override
	public JsonObject toDocument(Tag tag) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, tag.getName());
		addBasicReferences(document, tag);
		addTagFamily(document, tag.getTagFamily());
		addProject(document, tag.getProject());
		return document;
	}

	public void addTagFamily(JsonObject document, TagFamily tagFamily) {
		JsonObject info = new JsonObject();
		info.put(NAME_KEY, tagFamily.getName());
		info.put(UUID_KEY, tagFamily.getUuid());
		document.put("tagFamily", info);
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));

		// tagFamily
		JsonObject tagFamilyMapping = new JsonObject();
		tagFamilyMapping.put("type", OBJECT);
		JsonObject schemaMappingProperties = new JsonObject();
		schemaMappingProperties.put("uuid", fieldType(STRING, NOT_ANALYZED));
		schemaMappingProperties.put("name", fieldType(STRING, NOT_ANALYZED));
		tagFamilyMapping.put("properties", schemaMappingProperties);
		props.put("tagFamily", tagFamilyMapping);

		// project
		JsonObject projectMapping = new JsonObject();
		projectMapping.put("type", OBJECT);
		JsonObject projectMappingProps = new JsonObject();
		projectMappingProps.put("name", fieldType(STRING, NOT_ANALYZED));
		projectMappingProps.put("uuid", fieldType(STRING, NOT_ANALYZED));
		projectMapping.put("properties", projectMappingProps);
		props.put("project", projectMapping);

		return props;
	}

}
