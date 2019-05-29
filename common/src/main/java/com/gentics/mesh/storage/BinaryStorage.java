package com.gentics.mesh.storage;

import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * A binary storage provides means to store and retrieve binary data.
 */
public interface BinaryStorage {

	/**
	 * Store the upload in the temporary dir.
	 * 
	 * @param sourceFilePath
	 * @param temporaryId
	 * @return
	 */
	Completable storeInTemp(String sourceFilePath, String temporaryId);

	/**
	 * Stores the contents of the stream in the temporary location.
	 * 
	 * @param stream
	 * @param uuid
	 *            Uuid of the binary to be stored
	 * @param temporaryId
	 * @return
	 */
	Completable storeInTemp(Flowable<Buffer> stream, String temporaryId);

	/**
	 * Store the stream directly.
	 * 
	 * @param stream
	 * @param uuid
	 * @return
	 * @deprecated Use {@link #storeInTemp(Flowable, String, String)} in combination with {@link #moveInPlace(String, String)} instead.
	 */
	@Deprecated
	default Completable store(Flowable<Buffer> stream, String uuid) {
		String id = UUIDUtil.randomUUID();
		return storeInTemp(stream, id).andThen(moveInPlace(uuid, id));
	}

	/**
	 * Move the temporary uploaded binary into place.
	 * 
	 * @param uuid
	 * @param temporaryId
	 * @return
	 */
	Completable moveInPlace(String uuid, String temporaryId);

	/**
	 * Checks whether the binary data for the given field exists
	 * 
	 * @param binaryField
	 * @return
	 */
	boolean exists(BinaryGraphField field);

	/**
	 * Read the binary data which is identified by the given binary uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	Flowable<Buffer> read(String uuid);

	/**
	 * Read the entire binary data which is identified by the given binary uuid in the same thread blockingly.
	 *
	 * @param uuid
	 * @return
	 * @deprecated Use async variant instead
	 */
	@Deprecated
	Buffer readAllSync(String uuid);

	/**
	 * Return the local path to the binary if possible. Some storage implementations may only allow stream handling.
	 * 
	 * @param uuid
	 * @return
	 */
	default String getLocalPath(String uuid) {
		return null;
	}

	/**
	 * Delete the binary with the given uuid.
	 * 
	 * @param uuid
	 */
	Completable delete(String uuid);

	/**
	 * Delete the temporary upload with the given id.
	 * 
	 * @param temporaryId
	 * @return
	 */
	Completable purgeTemporaryUpload(String temporaryId);

}
