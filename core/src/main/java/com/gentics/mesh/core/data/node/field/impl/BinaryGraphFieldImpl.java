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
import com.gentics.mesh.dagger.MeshInternal;
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

		// The binary field does not yet exist but the update request already contains some binary field info. We can use this info to create a new binary
		// field. We locate the binary vertex by using the given hashsum. This case usually happens during schema migrations in which the binary graph field is
		// in fact initially being removed from the container.
		if (graphBinaryField == null) {
			// TODO fail here if the hashsum is missing.
			String hash = fieldMap.getBinaryField(fieldKey).getSha512sum();
			Binary binary = MeshInternal.get().boot().binaryRoot().findByHash(hash);
			graphBinaryField = container.createBinary(fieldKey, binary);
		}

		// Handle Update - Dominant Color
		if (binaryField.getDominantColor() != null) {
			graphBinaryField.setImageDominantColor(binaryField.getDominantColor());
		}

		// Handle Update - Filename
		if (binaryField.getFileName() != null) {
			if (isEmpty(binaryField.getFileName())) {
				throw error(BAD_REQUEST, "field_binary_error_emptyfilename", fieldKey);
			} else {
				graphBinaryField.setFileName(binaryField.getFileName());
			}
		}

		// Handle Update - MimeType
		if (binaryField.getMimeType() != null) {
			if (isEmpty(binaryField.getMimeType())) {
				throw error(BAD_REQUEST, "field_binary_error_emptymimetype", fieldKey);
			}
			graphBinaryField.setMimeType(binaryField.getMimeType());
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

	/**
	 * Remove the field from the given container. The attached binary will be be removed if no other container is referencing it. The data will be deleted from
	 * the binary storage as well.
	 */
	@Override
	public void removeField(GraphFieldContainer container) {
		Binary binary = getBinary();
		remove();
		// Only get rid of the binary as well if no other fields are using the binary.
		if (!binary.findFields().iterator().hasNext()) {
			binary.remove();
		}
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		BinaryGraphFieldImpl field = getGraph().addFramedEdge(container, getBinary(), HAS_FIELD, BinaryGraphFieldImpl.class);
		field.setFieldKey(getFieldKey());
		for (String key : getPropertyKeys()) {
			if (key.equals("uuid") || key.equals("ferma_type")) {
				continue;
			}
			Object value = getProperty(key);
			field.setProperty(key, value);
		}
		return field;
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
