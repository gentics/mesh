package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * @see Language
 */
public class LanguageImpl extends AbstractMeshCoreVertex<LanguageResponse> implements Language {

	public static final String LANGUAGE_TAG_PROPERTY_KEY = "languageTag";
	public static final String LANGUAGE_NATIVE_NAME_PROPERTY_KEY = "nativeName";
	public static final String LANGUAGE_NAME_PROPERTY_KEY = "name";

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(LanguageImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(LanguageImpl.class)
			.withField(LANGUAGE_TAG_PROPERTY_KEY, STRING)
			.unique());
	}

	@Override
	public String getName() {
		return property(LANGUAGE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setName(String name) {
		property(LANGUAGE_NAME_PROPERTY_KEY, name);
	}

	@Override
	public String getNativeName() {
		return property(LANGUAGE_NATIVE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setNativeName(String name) {
		property(LANGUAGE_NATIVE_NAME_PROPERTY_KEY, name);
	}

	@Override
	public String getLanguageTag() {
		return property(LANGUAGE_TAG_PROPERTY_KEY);
	}

	@Override
	public void setLanguageTag(String languageTag) {
		property(LANGUAGE_TAG_PROPERTY_KEY, languageTag);
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException();
	}

	@Override
	public boolean update(InternalActionContext rc, EventQueueBatch batch) {
		throw new NotImplementedException("Languages can't be updated");
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return "";
	}
}
