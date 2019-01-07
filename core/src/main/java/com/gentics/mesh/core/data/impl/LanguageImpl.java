package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.graphdb.spi.FieldType.STRING;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.util.ETag;

import io.reactivex.Single;

/**
 * @see Language
 */
public class LanguageImpl extends AbstractMeshCoreVertex<LanguageResponse, Language> implements Language {

	public static final String LANGUAGE_TAG_PROPERTY_KEY = "languageTag";
	public static final String LANGUAGE_NATIVE_NAME_PROPERTY_KEY = "nativeName";
	public static final String LANGUAGE_NAME_PROPERTY_KEY = "name";

	public static void init(LegacyDatabase database) {
		database.addVertexType(LanguageImpl.class, MeshVertexImpl.class);
		database.addVertexIndex(LanguageImpl.class, true, LANGUAGE_TAG_PROPERTY_KEY, STRING);
	}

	@Override
	public String getName() {
		return value(LANGUAGE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setName(String name) {
		property(LANGUAGE_NAME_PROPERTY_KEY, name);
	}

	@Override
	public String getNativeName() {
		return value(LANGUAGE_NATIVE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setNativeName(String name) {
		property(LANGUAGE_NATIVE_NAME_PROPERTY_KEY, name);
	}

	@Override
	public String getLanguageTag() {
		return value(LANGUAGE_TAG_PROPERTY_KEY);
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
	public void delete(BulkActionContext context) {
		throw new NotImplementedException();
	}

	@Override
	public boolean update(InternalActionContext rc, SearchQueueBatch batch) {
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
