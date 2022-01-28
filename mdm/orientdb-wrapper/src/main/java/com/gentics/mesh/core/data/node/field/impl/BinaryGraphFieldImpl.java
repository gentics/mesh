package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

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
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.impl.BinaryImpl;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
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

	/**
	 * Initialize the binary field edge index and type.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
	}

	@Override
	public HibBinaryField copyTo(HibBinaryField target) {
		for (String key : getPropertyKeys()) {
			// Don't copy the uuid
			if ("uuid".equals(key)) {
				continue;
			}
			Object value = property(key);
			((BinaryGraphField) target).property(key, value);
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
		Binary graphBinary = toGraph(getBinary());
		remove();
		// Only get rid of the binary as well if no other fields are using the binary.
		if (!graphBinary.findFields().hasNext()) {
			graphBinary.delete(bac);
		}
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		BinaryGraphFieldImpl field = getGraph().addFramedEdge(toGraph(container), toGraph(getBinary()), HAS_FIELD, BinaryGraphFieldImpl.class);
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
            name = name.replaceAll("%5B", "[");
            name = name.replaceAll("%5D", "]");
            String value = property(key);
            metadata.put(name, value);
        }
        return metadata;
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
