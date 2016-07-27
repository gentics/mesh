package com.gentics.mesh.core.data.node.field.impl;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import rx.Observable;

public class BinaryGraphFieldImpl extends MeshVertexImpl implements BinaryGraphField {

	private static final String BINARY_FILESIZE_PROPERTY_KEY = "binaryFileSize";

	private static final String BINARY_FILENAME_PROPERTY_KEY = "binaryFilename";

	private static final String BINARY_SHA512SUM_PROPERTY_KEY = "binarySha512Sum";

	private static final String BINARY_CONTENT_TYPE_PROPERTY_KEY = "binaryContentType";

	private static final String BINARY_IMAGE_DPI_PROPERTY_KEY = "binaryImageDPI";

	private static final String BINARY_IMAGE_WIDTH_PROPERTY_KEY = "binaryImageWidth";

	private static final String BINARY_IMAGE_HEIGHT_PROPERTY_KEY = "binaryImageHeight";

	@Override
	public Observable<BinaryField> transformToRest(com.gentics.mesh.handler.ActionContext ac) {
		BinaryField restModel = new BinaryFieldImpl();
		restModel.setFileName(getFileName());
		restModel.setMimeType(getMimeType());
		restModel.setFileSize(getFileSize());
		restModel.setSha512sum(getSHA512Sum());
		restModel.setDpi(getImageDPI());
		restModel.setWidth(getImageWidth());
		restModel.setHeight(getImageHeight());
		return Observable.just(restModel);
	}

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public String getSegmentedPath() {
		String[] parts = getUuid().split("(?<=\\G.{4})");
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		for (String part : parts) {
			buffer.append(part + File.separator);
		}
		return buffer.toString();
	}

	@Override
	public String getFilePath() {
		File folder = new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory(), getSegmentedPath());
		File binaryFile = new File(folder, getUuid() + ".bin");
		return binaryFile.getAbsolutePath();
	}

	@Override
	public boolean hasImage() {
		String contentType = getMimeType();
		if (contentType == null) {
			return false;
		}
		return contentType.startsWith("image/");
	}

	@Override
	public Integer getImageWidth() {
		return getProperty(BINARY_IMAGE_WIDTH_PROPERTY_KEY);
	}

	@Override
	public void setImageWidth(Integer width) {
		setProperty(BINARY_IMAGE_WIDTH_PROPERTY_KEY, width);
	}

	@Override
	public Integer getImageHeight() {
		return getProperty(BINARY_IMAGE_HEIGHT_PROPERTY_KEY);
	}

	@Override
	public void setImageHeight(Integer heigth) {
		setProperty(BINARY_IMAGE_HEIGHT_PROPERTY_KEY, heigth);
	}

	@Override
	public Integer getImageDPI() {
		return getProperty(BINARY_IMAGE_DPI_PROPERTY_KEY);
	}

	@Override
	public void setImageDPI(Integer dpi) {
		setProperty(BINARY_IMAGE_DPI_PROPERTY_KEY, dpi);
	}

	@Override
	public String getSHA512Sum() {
		return getProperty(BINARY_SHA512SUM_PROPERTY_KEY);
	}

	@Override
	public void setSHA512Sum(String sha512HashSum) {
		setProperty(BINARY_SHA512SUM_PROPERTY_KEY, sha512HashSum);
	}

	@Override
	public long getFileSize() {
		Long size = getProperty(BINARY_FILESIZE_PROPERTY_KEY);
		return size == null ? 0 : size;
	}

	@Override
	public void setFileSize(long sizeInBytes) {
		setProperty(BINARY_FILESIZE_PROPERTY_KEY, sizeInBytes);
	}

	@Override
	public void setFileName(String filenName) {
		setProperty(BINARY_FILENAME_PROPERTY_KEY, filenName);
	}

	@Override
	public String getFileName() {
		return getProperty(BINARY_FILENAME_PROPERTY_KEY);
	}

	@Override
	public String getMimeType() {
		return getProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY);
	}

	@Override
	public void setMimeType(String contentType) {
		setProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY, contentType);
	}

	@Override
	public Future<Buffer> getFileBuffer() {
		Future<Buffer> future = Future.future();
		Mesh.vertx().fileSystem().readFile(getFilePath(), rh -> {
			if (rh.succeeded()) {
				future.complete(rh.result());
			} else {
				future.fail(rh.cause());
			}
		});
		return future;
	}

	@Override
	public File getFile() {
		File binaryFile = new File(getFilePath());
		return binaryFile;
	}

	@Override
	public void removeField() {
		remove();
	}
}
