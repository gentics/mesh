package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.impl.BinaryImpl;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.core.rest.node.field.impl.BinaryFieldImpl;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.NodeUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see BinaryGraphField
 */
public class BinaryGraphFieldImpl extends MeshEdgeImpl implements BinaryGraphField {

	public static final Set<String> allowedTypes = new HashSet<>();

	private static final Logger log = LoggerFactory.getLogger(BinaryGraphFieldImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
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
		String hash = binaryField.getSha512sum();
		if (graphBinaryField == null && hash != null) {
			HibBinary binary = Tx.get().binaries().findByHash(hash).runInExistingTx(Tx.get());
			if (binary != null) {
				graphBinaryField = container.createBinary(fieldKey, binary);
			} else {
				log.debug("Could not find binary for hash {" + hash + "}");
			}
		}

		// Otherwise we can't update the binaryfield
		if (graphBinaryField == null && binaryField.hasValues()) {
			throw error(BAD_REQUEST, "field_binary_error_unable_to_set_before_upload", fieldKey);
		}

		// Handle Update - Dominant Color
		if (binaryField.getDominantColor() != null) {
			graphBinaryField.setImageDominantColor(binaryField.getDominantColor());
		}

		// Handle Update - Focal point
		FocalPoint newFocalPoint = binaryField.getFocalPoint();
		if (newFocalPoint != null) {
			HibBinary binary = graphBinaryField.getBinary();
			Point imageSize = binary.getImageSize();
			if (imageSize != null) {
				if (!newFocalPoint.convertToAbsolutePoint(imageSize).isWithinBoundsOf(imageSize)) {
					throw error(BAD_REQUEST, "field_binary_error_image_focalpoint_out_of_bounds", fieldKey, newFocalPoint.toString(),
						imageSize.toString());
				}
			}
			graphBinaryField.setImageFocalPoint(newFocalPoint);
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

		// Handle Update - Metadata
		BinaryMetadata metaData = binaryField.getMetadata();
		if (metaData != null) {
			graphBinaryField.clearMetadata();
			for (Entry<String, String> entry : metaData.getMap().entrySet()) {
				graphBinaryField.setMetadata(entry.getKey(), entry.getValue());
			}
			Location loc = metaData.getLocation();
			if (loc != null) {
				graphBinaryField.setLocation(loc);
			}
		}

		// Handle Update - Plain text
		String text = binaryField.getPlainText();
		if (text != null) {
			graphBinaryField.setPlainText(text);
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

		HibBinary binary = getBinary();
		if (binary != null) {
			restModel.setBinaryUuid(binary.getUuid());
			restModel.setFileSize(binary.getSize());
			restModel.setSha512sum(binary.getSHA512Sum());
			restModel.setWidth(binary.getImageWidth());
			restModel.setHeight(binary.getImageHeight());
		}

		restModel.setFocalPoint(getImageFocalPoint());
		restModel.setDominantColor(getImageDominantColor());

		BinaryMetadata metaData = getMetadata();
		restModel.setMetadata(metaData);

		restModel.setPlainText(getPlainText());
		return restModel;
	}

	@Override
	public BinaryGraphField copyTo(BinaryGraphField target) {
		for (String key : getPropertyKeys()) {
			// Don't copy the uuid
			if ("uuid".equals(key)) {
				continue;
			}
			Object value = property(key);
			target.property(key, value);
		}
		return this;
	}

	@Override
	public void setFieldKey(String key) {
		property(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public String getFieldKey() {
		return property(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public boolean hasProcessableImage() {
		return NodeUtil.isProcessableImage(getMimeType());
	}

	@Override
	public String getDisplayName() {
		return getFileName();
	}

	/**
	 * Remove the field from the given container. The attached binary will be be removed if no other container is referencing it. The data will be deleted from
	 * the binary storage as well.
	 */
	@Override
	public void removeField(BulkActionContext bac, GraphFieldContainer container) {
		Binary graphBinary = toGraph(getBinary());
		remove();
		// Only get rid of the binary as well if no other fields are using the binary.
		if (!graphBinary.findFields().hasNext()) {
			graphBinary.delete(bac);
		}
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		BinaryGraphFieldImpl field = getGraph().addFramedEdge(container, toGraph(getBinary()), HAS_FIELD, BinaryGraphFieldImpl.class);
		field.setFieldKey(getFieldKey());

		// Clone all properties except the uuid and the type.
		for (String key : getPropertyKeys()) {
			if (key.equals("uuid") || key.equals("ferma_type")) {
				continue;
			}
			Object value = property(key);
			field.property(key, value);
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

			HibBinary binaryA = getBinary();
			HibBinary binaryB = binaryField.getBinary();

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

			boolean matchingFocalPoint = true;
			if (binaryField.getFocalPoint() != null) {
				FocalPoint pointA = getImageFocalPoint();
				FocalPoint pointB = binaryField.getFocalPoint();
				matchingFocalPoint = Objects.equals(pointA, pointB);
			}

			boolean matchingDominantColor = true;
			if (binaryField.getDominantColor() != null) {
				String colorA = getImageDominantColor();
				String colorB = binaryField.getDominantColor();
				matchingDominantColor = Objects.equals(colorA, colorB);
			}

			boolean matchingSha512sum = true;
			if (binaryField.getSha512sum() != null) {
				String hashSumA = getBinary() != null ? getBinary().getSHA512Sum() : null;
				String hashSumB = binaryField.getSha512sum();
				matchingSha512sum = Objects.equals(hashSumA, hashSumB);
			}

			boolean matchingMetadata = true;
			if (binaryField.getMetadata() != null) {
				BinaryMetadata graphMetadata = getMetadata();
				BinaryMetadata restMetadata = binaryField.getMetadata();
				matchingMetadata = Objects.equals(graphMetadata, restMetadata);
			}
			return matchingFilename && matchingMimetype && matchingFocalPoint && matchingDominantColor && matchingSha512sum && matchingMetadata;
		}
		return false;
	}

	@Override
	public HibBinary getBinary() {
		return inV().nextOrDefaultExplicit(BinaryImpl.class, null);
	}

	@Override
	public Map<String, String> getMetadataProperties() {
		List<String> keys = getPropertyKeys().stream().filter(k -> k.startsWith(META_DATA_PROPERTY_PREFIX)).collect(Collectors.toList());

		Map<String, String> metadata = new HashMap<>();
		for (String key : keys) {
			String name = key.substring(META_DATA_PROPERTY_PREFIX.length());
			String value = property(key);
			metadata.put(name, value);
		}
		return metadata;
	}

	@Override
	public BinaryMetadata getMetadata() {
		BinaryMetadata metaData = new BinaryMetadata();
		for (Entry<String, String> entry : getMetadataProperties().entrySet()) {
			metaData.add(entry.getKey(), entry.getValue());
		}

		// Now set the GPS information
		Double lat = getLocationLatitude();
		Double lon = getLocationLongitude();
		if (lat != null && lon != null) {
			metaData.setLocation(lon, lat);
		}
		Integer alt = getLocationAltitude();
		if (alt != null && metaData.getLocation() != null) {
			metaData.getLocation().setAlt(alt);
		}
		return metaData;
	}

	@Override
	public void setMetadata(String key, String value) {
		setProperty(META_DATA_PROPERTY_PREFIX + key, value);
	}

	@Override
	public String getPlainText() {
		return getProperty(PLAIN_TEXT_KEY);
	}

	@Override
	public void setPlainText(String text) {
		setProperty(PLAIN_TEXT_KEY, text);
	}

}
