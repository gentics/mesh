package com.gentics.mesh.core.data.binary.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_VARIANTS;
import static com.gentics.mesh.madl.field.FieldType.LINK;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.binary.BinaryGraphFieldVariant;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;

/**
 * An edge to the Image variants.
 * 
 * @author plyhun
 *
 */
public class BinaryGraphFieldVariantImpl extends MeshEdgeImpl implements BinaryGraphFieldVariant {

	/**
	 * Initialize the variant field edge index and type.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(edgeType(BinaryGraphFieldVariantImpl.class.getSimpleName()));
		type.createType(edgeType(HAS_FIELD_VARIANTS).withSuperClazz(BinaryGraphFieldVariantImpl.class));

//		index.createIndex(edgeIndex(HAS_FIELD_VARIANTS)
//			.withPostfix("variant")
//			.withField("out", LINK)
//			.withField("in", LINK));
	}

	@Override
	public ImageVariant getVariant() {
		return inV().nextOrDefaultExplicit(ImageVariantImpl.class, null);
	}

	public BinaryGraphField getBinaryField() {
		return outV().nextExplicit(NodeGraphFieldContainerImpl.class).getBinary(getFieldKey());
	}
}
