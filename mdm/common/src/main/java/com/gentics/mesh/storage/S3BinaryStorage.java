package com.gentics.mesh.storage;

import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.parameter.ImageManipulationParameters;
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
	 * Read the binary data which is identified by the given binary uuid.
	 *
	 * @param objectKey
	 * @return
	 */
	Flowable<Buffer> read(String objectKey);


	/**
	 * Create a presigned URL that can be used for a given time period.
	 *
	 * @param nodeUuid
	 * @param fieldName
	 * @return
	 */
	Single<S3RestResponse> createPresignedUrl(String bucketName, String nodeUuid, String fieldName);
	Single<S3RestResponse> getPresignedUrl(String bucketName, String objectKey);
	 Flowable<Buffer> read(String objectKey,  ImageManipulationParameters parameters);
	 void createCacheBucket(String cacheBucketName);
	 Single<S3RestResponse> write(String bucket, String objectKey, File file);
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
