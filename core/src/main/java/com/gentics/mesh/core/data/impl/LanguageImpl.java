package com.gentics.mesh.core.data.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

public class LanguageImpl extends AbstractGenericVertex<LanguageResponse>implements Language {

	@Override
	public String getType() {
		return Language.TYPE;
	}

	// TODO add index
	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getNativeName() {
		return getProperty("nativeName");
	}

	@Override
	public void setNativeName(String name) {
		setProperty("nativeName", name);
	}

	@Override
	public String getLanguageTag() {
		return getProperty("languageTag");
	}

	@Override
	public void setLanguageTag(String languageTag) {
		setProperty("languageTag", languageTag);
	}

	@Override
	public Language transformToRest(RoutingContext rc, Handler<AsyncResult<LanguageResponse>> handler) {
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

	public void update(RoutingContext rc) {
		throw new NotImplementedException();
	}
}
