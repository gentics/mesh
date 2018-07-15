package com.gentics.mesh.core.binary.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.handler.ActionContext;

import io.reactivex.Completable;
import io.vertx.ext.web.FileUpload;

@Singleton
public class BasicUploadDataProcessor extends AbstractBinaryProcessor {

	@Inject
	public BasicUploadDataProcessor() {
	}

	@Override
	public boolean accepts(String contentType) {
		return true;
	}

	@Override
	public Completable process(ActionContext ac, FileUpload upload, BinaryGraphField field) {
		field.setFileName(upload.fileName());
		field.getBinary().setSize(upload.size());
		field.setMimeType(upload.contentType());
		return Completable.complete();
	}
}
