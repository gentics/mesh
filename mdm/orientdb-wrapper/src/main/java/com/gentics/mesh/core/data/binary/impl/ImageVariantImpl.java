package com.gentics.mesh.core.data.binary.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.*;
import static com.gentics.mesh.madl.field.FieldType.FLOAT;
import static com.gentics.mesh.madl.field.FieldType.INTEGER;
import static com.gentics.mesh.madl.field.FieldType.LINK;
import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.madl.field.FieldType;

/**
 * @see ImageVariant
 * 
 * @author plyhun
 *
 */
@GraphElement
public class ImageVariantImpl extends MeshVertexImpl implements ImageVariant {

	/**
	 * Initialize the edge type and index.
	 *
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ImageVariantImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(ImageVariantImpl.class)
			.withField(ImageVariant.WIDTH_KEY, FieldType.INTEGER)
			.withField(ImageVariant.HEIGHT_KEY, FieldType.INTEGER)
			.withField(ImageVariant.AUTO_KEY, FieldType.BOOLEAN)
			.withField(ImageVariant.CROP_MODE_KEY, FieldType.STRING)
			.withField(ImageVariant.CROP_X_KEY, FieldType.INTEGER)
			.withField(ImageVariant.CROP_Y_KEY, FieldType.INTEGER)
			.withField(ImageVariant.FOCAL_POINT_X_KEY, FieldType.FLOAT)
			.withField(ImageVariant.FOCAL_POINT_Y_KEY, FieldType.FLOAT)
			.withField(ImageVariant.FOCAL_POINT_ZOOM_KEY, FieldType.FLOAT)
			.withField(ImageVariant.RESIZE_MODE_KEY, FieldType.STRING)
			.unique());
	}

	@Override
	public Result<? extends BinaryGraphField> findFields() {
		return getBinary().findFields();
	}

	@Override
	public Binary getBinary() {
		return in(HAS_VARIANTS).nextExplicit(BinaryImpl.class);
	}

	@Override
	public Object getBinaryDataId() {
		return getId();
	}
}
