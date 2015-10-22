package com.gentics.mesh.core.data.impl;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class LanguageImpl extends AbstractGenericVertex<LanguageResponse>implements Language {

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
	public Language transformToRest(InternalActionContext ac, Handler<AsyncResult<LanguageResponse>> handler) {
		LanguageResponse model = new LanguageResponse();
		model.setUuid(getUuid());
		model.setLanguageTag(getLanguageTag());
		model.setName(getName());
		model.setNativeName(getNativeName());

		handler.handle(Future.succeededFuture(model));
		return this;
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
	public void update(InternalActionContext rc, Handler<AsyncResult<Void>> handler) {
		throw new NotImplementedException();
	}

}
