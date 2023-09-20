package com.gentics.mesh.core.data.binary.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_VARIANTS;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VARIANTS;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.util.StreamUtil;

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
			.withField(ImageVariant.CROP_WIDTH_KEY, FieldType.INTEGER)
			.withField(ImageVariant.CROP_HEIGHT_KEY, FieldType.INTEGER)
			.withField(ImageVariant.FOCAL_POINT_X_KEY, FieldType.FLOAT)
			.withField(ImageVariant.FOCAL_POINT_Y_KEY, FieldType.FLOAT)
			.withField(ImageVariant.FOCAL_POINT_ZOOM_KEY, FieldType.FLOAT)
			.withField(ImageVariant.RESIZE_MODE_KEY, FieldType.STRING)
			.unique());
	}

	@Override
	public Result<? extends BinaryGraphField> findFields() {
		return new TraversalResult<>(StreamUtil.toStream(inE(HAS_FIELD_VARIANTS).frameExplicit(BinaryGraphFieldVariantImpl.class)).map(BinaryGraphFieldVariantImpl::getBinaryField));
	}

	@Override
	public Binary getBinary() {
		return in(HAS_VARIANTS).nextExplicit(BinaryImpl.class);
	}

	@Override
	public Object getBinaryDataId() {
		return getId();
	}

	@Override
	public void delete(BulkActionContext context) {
		getElement().remove();
	}
}
