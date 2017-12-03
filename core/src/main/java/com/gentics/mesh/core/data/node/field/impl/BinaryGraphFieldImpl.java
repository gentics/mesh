package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Objects;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.impl.BinaryImpl;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;

/**
 * @see BinaryGraphField
 */
public class BinaryGraphFieldImpl extends MeshEdgeImpl implements BinaryGraphField {

	public static void init(Database database) {
		// database.addVertexType(BinaryGraphFieldImpl.class, MeshVertexImpl.class);
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
		Binary currentBinary = graphBinaryField.getBinary();
		BinaryGraphField newGraphBinaryField = container.createBinary(fieldKey, currentBinary);

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

	@Override
	public BinaryField transformToRest(ActionContext ac) {
		BinaryField restModel = new BinaryFieldImpl();
		restModel.setFileName(getFileName());
		restModel.setMimeType(getMimeType());

		Binary binary = getBinary();
		if (binary != null) {
			restModel.setFileSize(binary.getSize());
			restModel.setSha512sum(binary.getSHA512Sum());
			restModel.setWidth(binary.getImageWidth());
			restModel.setHeight(binary.getImageHeight());
		}

		restModel.setDominantColor(getImageDominantColor());
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

	// @Override
	// public String getSegmentedPath() {
	// String[] parts = getUuid().split("(?<=\\G.{4})");
	// StringBuffer buffer = new StringBuffer();
	// buffer.append(File.separator);
	// for (String part : parts) {
	// buffer.append(part + File.separator);
	// }
	// return buffer.toString();
	// }
	//
	// @Override
	// public String getFilePath() {
	// File folder = new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory(), getSegmentedPath());
	// File binaryFile = new File(folder, getUuid() + ".bin");
	// return binaryFile.getAbsolutePath();
	// }

	@Override
	public boolean hasImage() {
		String contentType = getMimeType();
		if (contentType == null) {
			return false;
		}
		return contentType.startsWith("image/");
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

	// @Override
	// public String getSHA512Sum() {
	// return getProperty(BINARY_SHA512SUM_PROPERTY_KEY);
	// }
	//
	// @Override
	// public BinaryGraphField setSHA512Sum(String sha512HashSum) {
	// setProperty(BINARY_SHA512SUM_PROPERTY_KEY, sha512HashSum);
	// return this;
	// }
	//
	// @Override
	// public long getFileSize() {
	// Long size = getProperty(BINARY_FILESIZE_PROPERTY_KEY);
	// return size == null ? 0 : size;
	// }
	//
	// @Override
	// public BinaryGraphField setFileSize(long sizeInBytes) {
	// setProperty(BINARY_FILESIZE_PROPERTY_KEY, sizeInBytes);
	// return this;
	// }

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
	public String getMimeType() {
		return getProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY);
	}

	@Override
	public BinaryGraphField setMimeType(String contentType) {
		setProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY, contentType);
		return this;
	}

	@Override
	public void removeField(GraphFieldContainer container) {

		if (!getBinary().findFields().iterator().hasNext()) {
			// TODO delete the binary data as well
			getBinary().remove();
		}
		remove();
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		BinaryGraphFieldImpl edge = getGraph().addFramedEdge(container, getBinary(), HAS_FIELD, BinaryGraphFieldImpl.class);
		edge.setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, getFieldKey());
		// TODO clone properties
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

			Binary binaryA = getBinary();
			Binary binaryB = binaryField.getBinary();

			String hashSumA = binaryA != null ? binaryA.getSHA512Sum() : null;
			String hashSumB = binaryB != null ? binaryB.getSHA512Sum() : null;
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
				String hashSumA = getBinary() != null ? getBinary().getSHA512Sum() : null;
				String hashSumB = binaryField.getSha512sum();
				matchingSha512sum = Objects.equals(hashSumA, hashSumB);
			}
			return matchingFilename && matchingMimetype && matchingSha512sum;
		}
		return false;
	}

	@Override
	public Binary getBinary() {
		return inV().nextOrDefaultExplicit(BinaryImpl.class, null);
	}

}
