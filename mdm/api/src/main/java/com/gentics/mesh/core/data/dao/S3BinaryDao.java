package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.util.UUIDUtil;

import java.util.stream.Stream;

/**
 * DAO for {@link S3HibBinary}.
 */
public interface S3BinaryDao extends Dao<S3HibBinary>{
    /**
     * Find the s3 binary with the given s3ObjectKey.
     *
     * @param s3ObjectKey
     * @return
     */
    Transactional<? extends S3HibBinary> findByS3ObjectKey(String s3ObjectKey);

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
     * Return a stream of S3 binaries.
     *
     * @return
     */
    Transactional<Stream<S3HibBinary>> findAll();
}
