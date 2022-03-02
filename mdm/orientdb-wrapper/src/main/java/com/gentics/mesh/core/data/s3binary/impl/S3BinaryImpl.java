package com.gentics.mesh.core.data.s3binary.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibBinaryDataElement;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.node.field.impl.S3BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;

/**
 * @see S3Binary
 */
public class S3BinaryImpl extends MeshVertexImpl implements S3Binary {

	/**
	 * Initialize the vertex type and index.
	 *
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(S3BinaryImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(S3BinaryImpl.class)
			.withField(S3Binary.S3_AWS_OBJECT_KEY, FieldType.STRING)
			.unique());
	}

	@Override
	public Result<? extends S3BinaryGraphField> findFields() {
		return inE(HAS_FIELD, S3BinaryGraphFieldImpl.class);
	}

	@Override
	public void delete(BulkActionContext bac) {
		S3BinaryStorage storage = mesh().s3binaryStorage();
		bac.add(storage.delete(getS3ObjectKey()));
		bac.add(onDeleted(getUuid(), getS3ObjectKey()));
		getElement().remove();
	}

	@Override
	public HibBinaryDataElement setSize(long sizeInBytes) {
		return setSize(Long.valueOf(sizeInBytes));
	}
}
