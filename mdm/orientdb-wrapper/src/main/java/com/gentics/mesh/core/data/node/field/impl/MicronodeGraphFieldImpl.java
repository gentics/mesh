package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see MicronodeGraphField
 */
public class MicronodeGraphFieldImpl extends MeshEdgeImpl implements MicronodeGraphField {

	private static final Logger log = LoggerFactory.getLogger(MicronodeGraphFieldImpl.class);

	/**
	 * Create the micronode type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(edgeType(MicronodeGraphFieldImpl.class.getSimpleName()));
		type.createType(edgeType(HAS_FIELD).withSuperClazz(MicronodeGraphFieldImpl.class));
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
	public HibMicronode getMicronode() {
		return inV().has(MicronodeImpl.class).nextOrDefaultExplicit(MicronodeImpl.class, null);
	}

	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		HibMicronode micronode = getMicronode();
		// Remove the edge to get rid of the reference
		remove();
		if (micronode != null) {
			// Remove the micronode if this was the last edge to the micronode
			if (!toGraph(micronode).in(HAS_FIELD).hasNext()) {
				toGraph(micronode).delete(bac);
			}
		}
	}

	@Override
	public HibField cloneTo(HibFieldContainer container) {
		HibMicronode micronode = getMicronode();

		MicronodeGraphField field = getGraph().addFramedEdge(toGraph(container), toGraph(micronode), HAS_FIELD, MicronodeGraphFieldImpl.class);
		field.setFieldKey(getFieldKey());
		return field;
	}

	@Override
	public boolean equals(Object obj) {
		return micronodeFieldEquals(obj);
	}
}
