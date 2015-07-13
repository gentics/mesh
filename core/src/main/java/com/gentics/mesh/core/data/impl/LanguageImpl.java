package com.gentics.mesh.core.data.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.service.transformation.TransformationParameters;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

public class LanguageImpl extends AbstractGenericNode<LanguageResponse> implements Language {

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
	public Language transformToRest(MeshAuthUser requestUser, Handler<AsyncResult<LanguageResponse>> handler, TransformationParameters... parameters) {
		// TODO Auto-generated method stub
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
}
