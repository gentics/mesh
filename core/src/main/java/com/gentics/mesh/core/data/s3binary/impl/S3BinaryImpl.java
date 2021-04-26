package com.gentics.mesh.core.data.s3binary.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.node.field.impl.S3BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.storage.S3BinaryStorage;

import java.util.Base64;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

/**
 * @see S3Binary
 */
public class S3BinaryImpl extends MeshVertexImpl implements S3Binary {

	private static final Base64.Encoder BASE64 = Base64.getEncoder();

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(S3BinaryImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(S3BinaryImpl.class)
			.withField(S3Binary.SHA512SUM_KEY, FieldType.STRING)
			.unique());
	}

	@Override
	public Result<S3BinaryGraphField> findFields() {
		// TODO inE should not return wildcard generics
		return (Result<S3BinaryGraphField>) (Result<?>) inE(HAS_FIELD, S3BinaryGraphFieldImpl.class);
	}

	@Override
	public void delete(BulkActionContext bac) {
		S3BinaryStorage storage = mesh().s3binaryStorage();
		bac.add(storage.delete(getS3ObjectKey()));
		getElement().remove();
	}

}
