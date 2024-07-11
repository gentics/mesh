package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.util.UUIDUtil;

import java.util.stream.Stream;

/**
 * DAO for {@link S3Binary}.
 */
public interface S3BinaryDao extends Dao<S3Binary>{
    /**
     * Find the s3 binary with the given s3ObjectKey.
     *
     * @param s3ObjectKey
     * @return
     */
    default Transactional<? extends S3Binary> findByS3ObjectKey(String s3ObjectKey) {
        return getS3HibBinaries().findByS3ObjectKey(s3ObjectKey);
    }

    /**
     * Create a new s3 binary.
     *
     * @param uuid
     *            Uuid of the s3 binary
     * @param objectKey
     *            aws object key the s3 binary
     * @return
     */
    default Transactional<? extends S3Binary> create(String uuid, String objectKey, String fileName) {
        return getS3HibBinaries().create(uuid, objectKey,fileName);
    }

    /**
     * Return a stream of S3 binaries.
     *
     * @return
     */
    default Transactional<Stream<S3Binary>> findAll() {
        return getS3HibBinaries().findAll();
    }

    /**
     * Create a new s3 binary.
     *
     * @param objectKey
     * @return
     */
    default Transactional<? extends S3Binary> create(String objectKey, String fileName) {
        return create(UUIDUtil.randomUUID(), objectKey, fileName);
    }

    /**
     * Return the s3 binaries
     *
     * @return
     */
    S3Binaries getS3HibBinaries();

}
