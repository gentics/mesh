package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.field.*;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.impl.S3BinaryImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.image.Point;
import com.gentics.mesh.core.rest.node.field.impl.S3BinaryFieldImpl;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadata;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.NodeUtil;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @see S3BinaryGraphField
 */
public class S3BinaryGraphFieldImpl extends MeshEdgeImpl implements S3BinaryGraphField {

	public static final Set<String> allowedTypes = new HashSet<>();

	private static final Logger log = LoggerFactory.getLogger(S3BinaryGraphFieldImpl.class);

	/**
	 * Initialize the s3binary field edge index and type.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
	}

	/** Transform S3 binary if not null
	 * @param container
	 * @param ac
	 * @param fieldKey
	 * @param fieldSchema
	 * @param languageTags
	 * @param level
	 * @param parentNode
	 */
	public static FieldTransformer<S3BinaryField> S3_BINARY_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		S3BinaryGraphField graphBinaryField = container.getS3Binary(fieldKey);
		if (graphBinaryField == null) {
			return null;
		} else {
			return graphBinaryField.transformToRest(ac);
		}
	};

	/** Update S3 binary
	 * @param container
	 * @param ac
	 * @param fieldMap
	 * @param fieldKey
	 * @param fieldSchema
	 * @param schema
	 */
	public static FieldUpdater S3_BINARY_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		S3BinaryGraphField graphS3BinaryField = container.getS3Binary(fieldKey);
		S3BinaryField s3binaryField = fieldMap.getS3BinaryField(fieldKey);
		boolean isS3BinaryFieldSetToNull = fieldMap.hasField(fieldKey) && s3binaryField == null && graphS3BinaryField != null;

		GraphField.failOnDeletionOfRequiredField(graphS3BinaryField, isS3BinaryFieldSetToNull, fieldSchema, fieldKey, schema);

		boolean restIsNull = s3binaryField == null;
		// The required check for binary fields is not enabled since binary fields can only be created using the field api

		// Handle Deletion
		if (isS3BinaryFieldSetToNull && graphS3BinaryField != null) {
			graphS3BinaryField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNull) {
			return;
		}

		// The S3binary field does not yet exist but the update request already contains some binary field info. We can use this info to create a new binary
		// field. We locate the binary vertex by using the given hashsum. This case usually happens during schema migrations in which the binary graph field is
		// in fact initially being removed from the container.
		String s3ObjectKey = s3binaryField.getS3ObjectKey();
		if (graphS3BinaryField == null && s3ObjectKey != null) {
			S3HibBinary binary = Tx.get().s3binaries().findByS3ObjectKey(s3ObjectKey).runInExistingTx(Tx.get());
			if (binary != null) {
				graphS3BinaryField = container.createS3Binary(fieldKey, binary);
			} else {
				log.debug("Could not find binary for s3ObjectKey {" + s3ObjectKey + "}");
			}
		}

		// Otherwise we can't update the s3binaryField
		if (graphS3BinaryField == null && s3binaryField.hasValues()) {
			throw error(BAD_REQUEST, "field_binary_error_unable_to_set_before_upload", fieldKey);
		}

		// Handle Update - Dominant Color
		if (s3binaryField.getDominantColor() != null) {
			graphS3BinaryField.setImageDominantColor(s3binaryField.getDominantColor());
		}

		// Handle Update - Focal point
		FocalPoint newFocalPoint = s3binaryField.getFocalPoint();
		if (newFocalPoint != null) {
			S3HibBinary binary = graphS3BinaryField.getS3Binary();
			Point imageSize = binary.getImageSize();
			if (imageSize != null) {
				if (!newFocalPoint.convertToAbsolutePoint(imageSize).isWithinBoundsOf(imageSize)) {
					throw error(BAD_REQUEST, "field_binary_error_image_focalpoint_out_of_bounds", fieldKey, newFocalPoint.toString(),
							imageSize.toString());
				}
			}
			graphS3BinaryField.setImageFocalPoint(newFocalPoint);
		}

		// Handle Update - Filename
		if (s3binaryField.getFileName() != null) {
			if (isEmpty(s3binaryField.getFileName())) {
				throw error(BAD_REQUEST, "field_binary_error_emptyfilename", fieldKey);
			} else {
				graphS3BinaryField.setFileName(s3binaryField.getFileName());
			}
		}

		// Handle Update - MimeType
		if (s3binaryField.getMimeType() != null) {
			if (isEmpty(s3binaryField.getMimeType())) {
				throw error(BAD_REQUEST, "field_binary_error_emptymimetype", fieldKey);
			}
			graphS3BinaryField.setMimeType(s3binaryField.getMimeType());
		}

		// Handle Update - Metadata
		S3BinaryMetadata metaData = s3binaryField.getMetadata();
		if (metaData != null) {
			graphS3BinaryField.clearMetadata();
			for (Entry<String, String> entry : metaData.getMap().entrySet()) {
				graphS3BinaryField.setMetadata(entry.getKey(), entry.getValue());
			}
			Location loc = metaData.getLocation();
			if (loc != null) {
				graphS3BinaryField.setLocation(loc);
			}
		}

		// Handle Update - Plain text
		String text = s3binaryField.getPlainText();
		if (text != null) {
			graphS3BinaryField.setPlainText(text);
		}

		// Handle Update - Plain text
		String key = s3binaryField.getS3ObjectKey();
		if (s3ObjectKey != null) {
			graphS3BinaryField.setS3ObjectKey(key);
		}


		// Don't update image width, height, SHA checksum - those are immutable
	};

	/** Get S3 Binary
	 * @param container
	 * @param fieldSchema
	 */
	public static FieldGetter S3_BINARY_GETTER = (container, fieldSchema) -> {
		return container.getS3Binary(fieldSchema.getName());
	};

	@Override
	public S3BinaryField transformToRest(ActionContext ac) {
		S3BinaryField restModel = new S3BinaryFieldImpl();

		S3HibBinary binary = getS3Binary();
		if (binary != null) {
			restModel.setS3binaryUuid(binary.getUuid());
			restModel.setS3ObjectKey(binary.getS3ObjectKey());
			restModel.setFileName(binary.getFileName());
		}
		S3BinaryMetadata metaData = getMetadata();
		restModel.setMetadata(metaData);
		return restModel;
	}

	@Override
	public S3BinaryGraphField copyTo(S3BinaryGraphField target) {
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
		S3Binary graphBinary = toGraph(getS3Binary());
		remove();
		// Only get rid of the binary as well if no other fields are using the binary.
		if (!graphBinary.findFields().hasNext()) {
			graphBinary.delete(bac);
		}
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		S3BinaryGraphFieldImpl field = getGraph().addFramedEdge(container, toGraph(getS3Binary()), HAS_FIELD, S3BinaryGraphFieldImpl.class);
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
		if (obj instanceof S3BinaryGraphField) {
			S3BinaryGraphField s3binaryField = (S3BinaryGraphField) obj;
			String filenameA = getFileName();
			String filenameB = s3binaryField.getFileName();
			boolean filename = Objects.equals(filenameA, filenameB);

			String mimeTypeA = getMimeType();
			String mimeTypeB = s3binaryField.getMimeType();
			boolean mimetype = Objects.equals(mimeTypeA, mimeTypeB);

			S3HibBinary s3binaryA = getS3Binary();
			S3HibBinary s3binaryB = s3binaryField.getS3Binary();

			String hashSumA = s3binaryA != null ? s3binaryA.getSHA512Sum() : null;
			String hashSumB = s3binaryB != null ? s3binaryB.getSHA512Sum() : null;
			boolean sha512sum = Objects.equals(hashSumA, hashSumB);
			return filename && mimetype && sha512sum;
		}
		if (obj instanceof S3BinaryField) {
			S3BinaryField s3binaryField = (S3BinaryField) obj;

			boolean matchingS3ObjectKey = true;
			if (s3binaryField.getS3ObjectKey() != null) {
				String s3ObjectKeyA = getS3ObjectKey();
				String s3ObjectKeyB = s3binaryField.getS3ObjectKey();
				matchingS3ObjectKey = Objects.equals(s3ObjectKeyA, s3ObjectKeyB);
			}

			boolean matchingFilename = true;
			if (s3binaryField.getFileName() != null) {
				String filenameA = getFileName();
				String filenameB = s3binaryField.getFileName();
				matchingFilename = Objects.equals(filenameA, filenameB);
			}

			boolean matchingMimetype = true;
			if (s3binaryField.getMimeType() != null) {
				String mimeTypeA = getMimeType();
				String mimeTypeB = s3binaryField.getMimeType();
				matchingMimetype = Objects.equals(mimeTypeA, mimeTypeB);
			}

			boolean matchingFocalPoint = true;
			if (s3binaryField.getFocalPoint() != null) {
				FocalPoint pointA = getImageFocalPoint();
				FocalPoint pointB = s3binaryField.getFocalPoint();
				matchingFocalPoint = Objects.equals(pointA, pointB);
			}

			boolean matchingDominantColor = true;
			if (s3binaryField.getDominantColor() != null) {
				String colorA = getImageDominantColor();
				String colorB = s3binaryField.getDominantColor();
				matchingDominantColor = Objects.equals(colorA, colorB);
			}

			boolean matchingSha512sum = true;
			if (s3binaryField.getSha512sum() != null) {
				String hashSumA = getS3Binary() != null ? getS3Binary().getSHA512Sum() : null;
				String hashSumB = s3binaryField.getSha512sum();
				matchingSha512sum = Objects.equals(hashSumA, hashSumB);
			}

			boolean matchingMetadata = true;
			if (s3binaryField.getMetadata() != null) {
				S3BinaryMetadata graphMetadata = getMetadata();
				S3BinaryMetadata restMetadata = s3binaryField.getMetadata();
				matchingMetadata = Objects.equals(graphMetadata, restMetadata);
			}
			return matchingFilename && matchingMimetype && matchingFocalPoint && matchingDominantColor && matchingSha512sum && matchingMetadata && matchingS3ObjectKey;
		}
		return false;
	}

	@Override
	public S3HibBinary getS3Binary() {
		return inV().nextOrDefaultExplicit(S3BinaryImpl.class, null);
	}

	@Override
    public Map<String, String> getMetadataProperties() {
        List<String> keys = getPropertyKeys().stream().filter(k -> k.startsWith(META_DATA_PROPERTY_PREFIX)).collect(Collectors.toList());
        Map<String, String> metadata = new HashMap<>();
        for (String key : keys) {
            String name = key.substring(META_DATA_PROPERTY_PREFIX.length());
            name = name.replaceAll("%5B", "[");
            name = name.replaceAll("%5D", "]");
            String value = property(key);
            metadata.put(name, value);
        }
        return metadata;
    }

	@Override
	public S3BinaryMetadata getMetadata() {
		S3BinaryMetadata metaData = new S3BinaryMetadata();
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
        key = key.replaceAll("\\[", "%5B");
        key = key.replaceAll("\\]", "%5D");
        if (value == null) {
            removeProperty(META_DATA_PROPERTY_PREFIX + key);
        } else {
            setProperty(META_DATA_PROPERTY_PREFIX + key, value);
        }
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
