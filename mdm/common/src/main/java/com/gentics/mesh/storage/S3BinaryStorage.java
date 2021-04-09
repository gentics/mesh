package com.gentics.mesh.storage;

import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;

import java.io.File;

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
	 * @param nodeUuid
	 * @param fieldName
	 * @return
	 */
	Single<S3RestResponse> createUploadPresignedUrl(String bucketName, String nodeUuid, String fieldName, boolean isCache);

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
	Single<S3RestResponse> uploadFile(String bucketName, String objectKey, File file);

	/**
	 * Check if objectKey in a bucket exists.
	 *
	 * @param bucketName
	 * @param objectKey
	 * @return
	 */
	Single<Boolean> exists(String bucketName, String objectKey);

	/**
         * Creates a S3 bucket for a given name if doesn't exists..
         *
         * @param bucketName
         * @return
         */
	 void createBucket(String bucketName);


	/**
	 * Delete the binary with the given uuid.
	 * 
	 * @param uuid
	 */
	Completable delete(String uuid);

}
