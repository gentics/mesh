package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_VARIANTS;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.BinaryGraphFieldVariant;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.binary.impl.BinaryImpl;
import com.gentics.mesh.core.data.binary.impl.ImageVariantImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;

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

	/**
	 * Remove the field from the given container. The attached binary will be be removed if no other container is referencing it. The data will be deleted from
	 * the binary storage as well.
	 */
	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		GraphFieldContainer graphContainer = toGraph(container);
		for (BinaryGraphFieldVariant variant : graphContainer.findImageVariants(getFieldKey())) {
			graphContainer.detachImageVariant(getFieldKey(), variant.getVariant());
		}
		Binary graphBinary = toGraph(getBinary());
		remove();
		// Only get rid of the binary as well if no other fields are using the binary.
		if (!graphBinary.findFields().hasNext()) {
			for (ImageVariant variant : graphBinary.getVariants()) {
				CommonTx.get().imageVariantDao().deletePersistedVariant(graphBinary, variant, true);
			}
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
		for (ImageVariant variant : getImageVariants()) {
			field.attachImageVariant(variant, false);
		}
		return field;
	}

	@Override
	public boolean equals(Object obj) {
		return binaryFieldEquals(obj);
	}

	@Override
	public Binary getBinary() {
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

	@Override
	public Result<? extends ImageVariant> getImageVariants()  {
		return new TraversalResult<>(
			getParentContainer()
				.outE(HAS_FIELD_VARIANTS)
				.has(GraphField.FIELD_KEY_PROPERTY_KEY, getFieldKey())
				.inV()
				.frameExplicit(ImageVariantImpl.class));
	}

	@Override
	public NodeGraphFieldContainerImpl getParentContainer() {
		return outV().nextExplicit(NodeGraphFieldContainerImpl.class);
	}
}
