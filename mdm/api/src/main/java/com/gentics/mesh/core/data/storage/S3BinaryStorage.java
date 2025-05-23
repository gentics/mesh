package com.gentics.mesh.core.data.storage;

import java.io.File;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.reactivex.core.buffer.Buffer;

/**
 * A S3 binary storage provides means to store and retrieve binary data.
 */
public interface S3BinaryStorage {

	/**
	 * Read the S3 binary data which is identified by the given bucketname and objectkey.
	 *
	 * @param bucketName
	 * @param objectKey
	 * @return
	 */
	Flowable<Buffer> read(String bucketName, String objectKey);


	/**
	 * Create a presigned URL that can be used for a given time period.
	 *
	 * @param bucketName
	 * @param objectKey
	 * @param isCache
	 * @return
	 */
	Single<S3RestResponse> createUploadPresignedUrl(String bucketName, String objectKey, String nodeVersion, boolean isCache);

	/**
	 * Get a presigned URL that can be used for a given time period.
	 *
	 * @param bucketName
	 * @param objectKey
	 * @return
	 */
	Single<S3RestResponse> createDownloadPresignedUrl(String bucketName, String objectKey, boolean isCache);

	/**
	 * Upload file into a s3bucket using the objectKey.
	 *
	 * @param bucketName
	 * @param objectKey
	 * @param file
	 * @return
	 */
	Single<S3RestResponse> uploadFile(String bucketName, String objectKey, File file, boolean isCache);

	/**
	 * Check if objectKey in a bucket exists.
	 *
	 * @param bucketName
	 * @param objectKey
	 * @return
	 */
	Single<Boolean> exists(String bucketName, String objectKey);

	/**
	 * Check if bucket exists.
	 *
	 * @param bucketName
	 * @return
	 */
	Single<Boolean> exists(String bucketName);


	/**
         * Creates a S3 bucket for a given name if doesn't exists..
         *
         * @param bucketName
         * @return
         */
	Single<Boolean> createBucket(String bucketName);

	/**
	 * Delete the S3 binary with the given bucket name and s3 object key.
	 * 
	 * @param bucket
	 * @param s3ObjectKey
	 */
	Completable delete(String bucket, String s3ObjectKey);

	/**
	 * Delete the S3 binary in the default bucket and s3 object key.
	 *
	 * @param s3ObjectKey
	 */
	Completable delete(String s3ObjectKey);

	/**
	 * Delete the binary with the given S3 key if the transaction succeeds.
	 * 
	 * @param s3ObjectKey
	 * @param tx
	 */
	void deleteOnTxSuccess(String s3ObjectKey, Tx tx);


	/**
	 * Make a S3 Object key from the params.
	 * 
	 * @param nodeUuid
	 * @param fieldName
	 * @param languageTag
	 * @return
	 */
	static String makeObjectKey(String nodeUuid, String fieldName, String languageTag) {
		return nodeUuid + "/" + fieldName + "/" + languageTag;
	}
}
