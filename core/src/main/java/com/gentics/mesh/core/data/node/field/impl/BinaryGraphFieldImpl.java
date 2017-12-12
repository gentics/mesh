package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.util.Objects;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import rx.Single;

public class BinaryGraphFieldImpl extends MeshVertexImpl implements BinaryGraphField {

	public static void init(Database database) {
		database.addVertexType(BinaryGraphFieldImpl.class, MeshVertexImpl.class);
	}

	public static FieldTransformer<BinaryField> BINARY_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		BinaryGraphField graphBinaryField = container.getBinary(fieldKey);
		if (graphBinaryField == null) {
			return null;
		} else {
			return graphBinaryField.transformToRest(ac);
		}
	};

	public static FieldUpdater BINARY_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		BinaryGraphField graphBinaryField = container.getBinary(fieldKey);
		BinaryField binaryField = fieldMap.getBinaryField(fieldKey);
		boolean isBinaryFieldSetToNull = fieldMap.hasField(fieldKey) && binaryField == null && graphBinaryField != null;

		GraphField.failOnDeletionOfRequiredField(graphBinaryField, isBinaryFieldSetToNull, fieldSchema, fieldKey, schema);

		boolean restIsNull = binaryField == null;
		// The required check for binary fields is not enabled since binary fields can only be created using the field api

		// Handle Deletion
		if (isBinaryFieldSetToNull && graphBinaryField != null) {
			graphBinaryField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// Always create a new binary field since each update must create a new field instance. The old field must be detached from the given container.
		BinaryGraphField newGraphBinaryField = container.createBinary(fieldKey);

		// Handle Update - Dominant Color
		if (binaryField.getDominantColor() != null) {
			newGraphBinaryField.setImageDominantColor(binaryField.getDominantColor());
		}

		// Handle Update - Filename
		if (binaryField.getFileName() != null) {
			if (isEmpty(binaryField.getFileName())) {
				throw error(BAD_REQUEST, "field_binary_error_emptyfilename", fieldKey);
			} else {
				newGraphBinaryField.setFileName(binaryField.getFileName());
			}
		}

		// Handle Update - MimeType
		if (binaryField.getMimeType() != null) {
			if (isEmpty(binaryField.getMimeType())) {
				throw error(BAD_REQUEST, "field_binary_error_emptymimetype", fieldKey);
			}
			newGraphBinaryField.setMimeType(binaryField.getMimeType());
		}
		// Don't update image width, height, SHA checksum - those are immutable
	};

	public static FieldGetter BINARY_GETTER = (container, fieldSchema) -> {
		return container.getBinary(fieldSchema.getName());
	};

	private static final String BINARY_FILESIZE_PROPERTY_KEY = "binaryFileSize";

	private static final String BINARY_FILENAME_PROPERTY_KEY = "binaryFilename";

	private static final String BINARY_SHA512SUM_PROPERTY_KEY = "binarySha512Sum";

	private static final String BINARY_CONTENT_TYPE_PROPERTY_KEY = "binaryContentType";

	private static final String BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY = "binaryImageDominantColor";

	private static final String BINARY_IMAGE_WIDTH_PROPERTY_KEY = "binaryImageWidth";

	private static final String BINARY_IMAGE_HEIGHT_PROPERTY_KEY = "binaryImageHeight";

	@Override
	public BinaryField transformToRest(ActionContext ac) {
		BinaryField restModel = new BinaryFieldImpl();
		restModel.setFileName(getFileName());
		restModel.setMimeType(getMimeType());
		restModel.setFileSize(getFileSize());
		restModel.setSha512sum(getSHA512Sum());
		restModel.setDominantColor(getImageDominantColor());
		restModel.setWidth(getImageWidth());
		restModel.setHeight(getImageHeight());
		return restModel;
	}

	@Override
	public BinaryGraphField copyTo(BinaryGraphField target) {
		for (String key : getPropertyKeys()) {
			// Don't copy the uuid
			if ("uuid".equals(key)) {
				continue;
			}
			Object value = getProperty(key);
			target.setProperty(key, value);
		}
		return this;
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
	public BinaryGraphField setImageWidth(Integer width) {
		setProperty(BINARY_IMAGE_WIDTH_PROPERTY_KEY, width);
		return this;
	}

	@Override
	public Integer getImageHeight() {
		return getProperty(BINARY_IMAGE_HEIGHT_PROPERTY_KEY);
	}

	@Override
	public BinaryGraphField setImageHeight(Integer heigth) {
		setProperty(BINARY_IMAGE_HEIGHT_PROPERTY_KEY, heigth);
		return this;
	}

	@Override
	public String getImageDominantColor() {
		return getProperty(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY);
	}

	@Override
	public BinaryGraphField setImageDominantColor(String dominantColor) {
		setProperty(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY, dominantColor);
		return this;
	}

	@Override
	public String getSHA512Sum() {
		return getProperty(BINARY_SHA512SUM_PROPERTY_KEY);
	}

	@Override
	public BinaryGraphField setSHA512Sum(String sha512HashSum) {
		setProperty(BINARY_SHA512SUM_PROPERTY_KEY, sha512HashSum);
		return this;
	}

	@Override
	public long getFileSize() {
		Long size = getProperty(BINARY_FILESIZE_PROPERTY_KEY);
		return size == null ? 0 : size;
	}

	@Override
	public BinaryGraphField setFileSize(long sizeInBytes) {
		setProperty(BINARY_FILESIZE_PROPERTY_KEY, sizeInBytes);
		return this;
	}

	@Override
	public BinaryGraphField setFileName(String filenName) {
		setProperty(BINARY_FILENAME_PROPERTY_KEY, filenName);
		return this;
	}

	@Override
	public String getFileName() {
		return getProperty(BINARY_FILENAME_PROPERTY_KEY);
	}

	@Override
	public String display() {
		return getFileName();
	}

	@Override
	public String getMimeType() {
		return getProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY);
	}

	@Override
	public BinaryGraphField setMimeType(String contentType) {
		setProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY, contentType);
		return this;
	}

	@Override
	public Single<AsyncFile> getFileStream() {
		return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<AsyncFile>(fut -> {
			Mesh.vertx().fileSystem().open(getFilePath(), new OpenOptions().setRead(true), fut);
		}));
	}

	@Override
	public File getFile() {
		File binaryFile = new File(getFilePath());
		return binaryFile;
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		// Detach the list from the given graph field container
		container.unlinkOut(this, HAS_FIELD);

		// Remove the field if no more containers are attached to it
		if (in(HAS_FIELD).count() == 0) {
			// delete(null);
			remove();
		}

	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		MeshEdgeImpl edge = getGraph().addFramedEdge(container, this, HAS_FIELD, MeshEdgeImpl.class);
		edge.setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, getFieldKey());
		return container.getBinary(getFieldKey());
	}

	@Override
	public void validate() {
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BinaryGraphField) {
			BinaryGraphField binaryField = (BinaryGraphField) obj;
			String filenameA = getFileName();
			String filenameB = binaryField.getFileName();
			boolean filename = Objects.equals(filenameA, filenameB);

			String mimeTypeA = getMimeType();
			String mimeTypeB = binaryField.getMimeType();
			boolean mimetype = Objects.equals(mimeTypeA, mimeTypeB);

			String hashSumA = getSHA512Sum();
			String hashSumB = binaryField.getSHA512Sum();
			boolean sha512sum = Objects.equals(hashSumA, hashSumB);
			return filename && mimetype && sha512sum;
		}
		if (obj instanceof BinaryField) {
			BinaryField binaryField = (BinaryField) obj;

			boolean matchingFilename = true;
			if (binaryField.getFileName() != null) {
				String filenameA = getFileName();
				String filenameB = binaryField.getFileName();
				matchingFilename = Objects.equals(filenameA, filenameB);
			}

			boolean matchingMimetype = true;
			if (binaryField.getMimeType() != null) {
				String mimeTypeA = getMimeType();
				String mimeTypeB = binaryField.getMimeType();
				matchingMimetype = Objects.equals(mimeTypeA, mimeTypeB);
			}
			boolean matchingSha512sum = true;
			if (binaryField.getSha512sum() != null) {
				String hashSumA = getSHA512Sum();
				String hashSumB = binaryField.getSha512sum();
				matchingSha512sum = Objects.equals(hashSumA, hashSumB);
			}
			return matchingFilename && matchingMimetype && matchingSha512sum;
		}
		return false;
	}

}
