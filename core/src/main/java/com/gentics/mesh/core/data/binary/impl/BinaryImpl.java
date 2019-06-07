package com.gentics.mesh.core.data.binary.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.syncleus.ferma.index.VertexIndexDefinition.vertexIndex;

import java.util.Base64;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.storage.BinaryStorage;
import com.syncleus.ferma.index.field.FieldType;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * @see Binary
 */
public class BinaryImpl extends MeshVertexImpl implements Binary {

	private static final Base64.Encoder BASE64 = Base64.getEncoder();

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(BinaryImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(BinaryImpl.class)
			.withField(Binary.SHA512SUM_KEY, FieldType.STRING)
			.unique());
	}

	@Override
	public TraversalResult<? extends BinaryGraphField> findFields() {
		return new TraversalResult<>(inE(HAS_FIELD).frameExplicit(BinaryGraphFieldImpl.class));
	}

	@Override
	public Flowable<Buffer> getStream() {
		BinaryStorage storage = MeshInternal.get().binaryStorage();
		return storage.read(getUuid());
	}

	@Override
	public String getBase64ContentSync() {
		Buffer buffer = MeshInternal.get().binaryStorage().readAllSync(getUuid());
		return BASE64.encodeToString(buffer.getBytes());
	}

	@Override
	public void delete(BulkActionContext bac) {
		BinaryStorage storage = MeshInternal.get().binaryStorage();
		bac.add(storage.delete(getUuid()));
		getElement().remove();
	}

}
