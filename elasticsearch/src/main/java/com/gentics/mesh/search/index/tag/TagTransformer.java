package com.gentics.mesh.search.index.tag;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.search.index.AbstractTransformer;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for tag search index documents.
 */
@Singleton
public class TagTransformer extends AbstractTransformer<Tag> {

	@Inject
	public TagTransformer() {
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

	/**
	 * Add the tag family fields to the document.
	 * 
	 * @param document
	 * @param tagFamily
	 */
	public void addTagFamily(JsonObject document, TagFamily tagFamily) {
		JsonObject info = new JsonObject();
		info.put(NAME_KEY, tagFamily.getName());
		info.put(UUID_KEY, tagFamily.getUuid());
		document.put("tagFamily", info);
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramStringType());

		// tagFamily
		JsonObject tagFamilyMapping = new JsonObject();
		tagFamilyMapping.put("type", OBJECT);
		JsonObject schemaMappingProperties = new JsonObject();
		schemaMappingProperties.put("uuid", notAnalyzedType(STRING));
		schemaMappingProperties.put("name", trigramStringType());
		tagFamilyMapping.put("properties", schemaMappingProperties);
		props.put("tagFamily", tagFamilyMapping);

		// project
		JsonObject projectMapping = new JsonObject();
		projectMapping.put("type", OBJECT);
		JsonObject projectMappingProps = new JsonObject();
		projectMappingProps.put("name", trigramStringType());
		projectMappingProps.put("uuid", notAnalyzedType(STRING));
		projectMapping.put("properties", projectMappingProps);
		props.put("project", projectMapping);

		return props;
	}

}
