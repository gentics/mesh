package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.field.*;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.s3binary.impl.S3BinaryImpl;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadata;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

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

	@Override
	public S3HibBinaryField copyTo(S3HibBinaryField target) {
		S3BinaryGraphField binaryGraphField = (S3BinaryGraphField) target;
		for (String key : getPropertyKeys()) {
			// Don't copy the uuid
			if ("uuid".equals(key)) {
				continue;
			}
			Object value = property(key);
			binaryGraphField.property(key, value);
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
	public String getDisplayName() {
		return getFileName();
	}

	/**
	 * Remove the field from the given container. The attached binary will be be removed if no other container is referencing it. The data will be deleted from
	 * the binary storage as well.
	 */
	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		S3Binary graphBinary = toGraph(getS3Binary());
		remove();
		// Only get rid of the binary as well if no other fields are using the binary.
		if (!graphBinary.findFields().hasNext()) {
			graphBinary.delete(bac);
		}
	}

	@Override
	public GraphField cloneTo(HibFieldContainer container) {
		GraphFieldContainer graphContainer = toGraph(container);
		S3BinaryGraphFieldImpl field = graphContainer.getGraph().addFramedEdge(graphContainer, toGraph(getS3Binary()), HAS_FIELD, S3BinaryGraphFieldImpl.class);
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

			String s3ObjectKeyA = s3binaryA != null ? s3binaryA.getS3ObjectKey() : null;
			String s3ObjectKeyB = s3binaryB != null ? s3binaryB.getS3ObjectKey() : null;
			boolean s3ObjectKey = Objects.equals(s3ObjectKeyA, s3ObjectKeyB);
			return filename && mimetype && s3ObjectKey;
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
			if (s3binaryField.getS3ObjectKey() != null) {
				String hashSumA = getS3Binary() != null ? getS3Binary().getS3ObjectKey() : null;
				String hashSumB = s3binaryField.getS3ObjectKey();
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
