package com.gentics.mesh.core.data.binary.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import java.io.InputStream;
import java.util.Base64;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.db.spi.Supplier;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.storage.BinaryStorage;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

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
	public Result<HibBinaryField> findFields() {
		// TODO inE should not return wildcard generics
		return (Result<HibBinaryField>) (Result<?>) inE(HAS_FIELD, BinaryGraphFieldImpl.class);
	}

	@Override
	public Flowable<Buffer> getStream() {
		BinaryStorage storage = mesh().binaryStorage();
		return storage.read(getUuid());
	}

	@Override
	public Supplier<InputStream> openBlockingStream() {
		BinaryStorage storage = mesh().binaryStorage();
		String uuid = getUuid();
		return () -> storage.openBlockingStream(uuid);
	}

	@Override
	public String getBase64ContentSync() {
		Buffer buffer = mesh().binaryStorage().readAllSync(getUuid());
		return BASE64.encodeToString(buffer.getBytes());
	}

	@Override
	public void delete(BulkActionContext bac) {
		BinaryStorage storage = mesh().binaryStorage();
		bac.add(storage.delete(getUuid()));
		getElement().remove();
	}

}
