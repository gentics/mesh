package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.graphdb.spi.Supplier;
import com.gentics.mesh.graphdb.spi.Transactional;
import com.gentics.mesh.util.UUIDUtil;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

import java.io.InputStream;
import java.util.stream.Stream;

/**
 * DAO for {@link S3HibBinary} operations.
 */
public interface S3BinaryDaoWrapper extends S3BinaryDao, DaoWrapper<S3HibBinary> {
	/**
	 * Find the s3 binary with the given hashsum.
	 *
	 * @param hash
	 * @return
	 */
	Transactional<? extends S3HibBinary> findByHash(String hash);

	/**
	 * Create a new s3 binary.
	 *
	 * @param uuid
	 *            Uuid of the s3 binary
	 * @param objectKey
	 *            aws object key the s3 binary
	 * @return
	 */
	Transactional<? extends S3HibBinary> create(String uuid, String objectKey, String fileName);

	/**
	 * Create a new s3 binary.
	 * 
	 * @param objectKey
	 * @return
	 */
	default Transactional<? extends S3HibBinary> create(String objectKey, String fileName) {
		return create(UUIDUtil.randomUUID(),objectKey,fileName);
	}

	/**
	 * Return a stream of binaries.
	 * 
	 * @return
	 */
	Transactional<Stream<S3HibBinary>> findAll();

	/**
	 * Find all s3binary fields which make use of this s3binary.
	 *
	 * @return
	 */
	Result<S3BinaryGraphField> findFields(S3HibBinary s3binary);

}
