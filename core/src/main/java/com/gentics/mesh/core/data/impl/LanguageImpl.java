package com.gentics.mesh.core.data.impl;

import static com.syncleus.ferma.index.VertexIndexDefinition.vertexIndex;
import static com.syncleus.ferma.index.field.FieldType.STRING;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;
import com.gentics.mesh.util.ETag;

import io.reactivex.Single;

/**
 * @see Language
 */
public class LanguageImpl extends AbstractMeshCoreVertex<LanguageResponse, Language> implements Language {

	public static final String LANGUAGE_TAG_PROPERTY_KEY = "languageTag";
	public static final String LANGUAGE_NATIVE_NAME_PROPERTY_KEY = "nativeName";
	public static final String LANGUAGE_NAME_PROPERTY_KEY = "name";

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
	public LanguageResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		LanguageResponse model = new LanguageResponse();
		model.setUuid(getUuid());
		model.setLanguageTag(getLanguageTag());
		model.setName(getName());
		model.setNativeName(getNativeName());
		return model;
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
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		// Languages don't have a public location
		return null;
	}

	@Override
	public Single<LanguageResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return DB.get().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}
}
