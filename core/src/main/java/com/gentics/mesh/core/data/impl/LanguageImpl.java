package com.gentics.mesh.core.data.impl;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

/**
 * @see Language
 */
public class LanguageImpl extends AbstractMeshCoreVertex<LanguageResponse, Language> implements Language {

	public static final String LANGUAGE_TAG_PROPERTY_KEY = "languageTag";
	public static final String LANGUAGE_NATIVE_NAME_PROPERTY_KEY = "nativeName";
	public static final String LANGUAGE_NAME_PROPERTY_KEY = "name";

	public static void checkIndices(Database database) {
		database.addVertexIndex(LanguageImpl.class, LANGUAGE_TAG_PROPERTY_KEY);
	}

	@Override
	public String getType() {
		return Language.TYPE;
	}

	// TODO add index
	@Override
	public String getName() {
		return getProperty(LANGUAGE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setName(String name) {
		setProperty(LANGUAGE_NAME_PROPERTY_KEY, name);
	}

	@Override
	public String getNativeName() {
		return getProperty(LANGUAGE_NATIVE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setNativeName(String name) {
		setProperty(LANGUAGE_NATIVE_NAME_PROPERTY_KEY, name);
	}

	@Override
	public String getLanguageTag() {
		return getProperty(LANGUAGE_TAG_PROPERTY_KEY);
	}

	@Override
	public void setLanguageTag(String languageTag) {
		setProperty(LANGUAGE_TAG_PROPERTY_KEY, languageTag);
	}

	@Override
	public Observable<LanguageResponse> transformToRestSync(InternalActionContext ac, String...languageTags) {
		LanguageResponse model = new LanguageResponse();
		model.setUuid(getUuid());
		model.setLanguageTag(getLanguageTag());
		model.setName(getName());
		model.setNativeName(getNativeName());
		return Observable.just(model);
	}

	@Override
	public LanguageImpl getImpl() {
		return this;
	}

	@Override
	public void delete() {
		throw new NotImplementedException();
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		new NotImplementedException();
	}

	@Override
	public Observable<Language> update(InternalActionContext rc) {
		throw new NotImplementedException();
	}

}
