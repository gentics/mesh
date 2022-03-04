package com.gentics.mesh.core.data.binary.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import java.util.Base64;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.madl.field.FieldType;

/**
 * @see Binary
 */
public class BinaryImpl extends MeshVertexImpl implements Binary {

	private static final Base64.Encoder BASE64 = Base64.getEncoder();

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(BinaryImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(BinaryImpl.class)
			.withField(Binary.SHA512SUM_KEY, FieldType.STRING)
			.unique());
	}

	@Override
	public Result<? extends HibBinaryField> findFields() {
		return inE(HAS_FIELD, BinaryGraphFieldImpl.class);
	}

	@Override
	public void delete(BulkActionContext bac) {
		BinaryStorage storage = mesh().binaryStorage();
		bac.add(storage.delete(getUuid()));
		getElement().remove();
	}

}
