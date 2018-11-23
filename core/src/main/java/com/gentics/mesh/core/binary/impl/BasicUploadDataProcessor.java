package com.gentics.mesh.core.binary.impl;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;

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
	public Consumer<BinaryGraphField> process(FileUpload upload) {
		return (field) -> {
			field.setFileName(upload.fileName());
			field.getBinary().setSize(upload.size());
			field.setMimeType(upload.contentType());
		};
	}
}
