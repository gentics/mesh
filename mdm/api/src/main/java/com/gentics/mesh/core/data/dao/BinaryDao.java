package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibBinaryField;
import com.gentics.mesh.core.result.Result;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * DAO for {@link HibBinary}.
 */
public interface BinaryDao extends DaoWrapper<HibBinary> {

	/**
	 * Return the binary data stream.
	 *
	 * @return
	 */
	Flowable<Buffer> getStream(HibBinary binary);

	/**
	 * Return the data as base 64 encoded string in the same thread blockingly.
	 *
	 * @return
	 */
	String getBase64ContentSync(HibBinary binary);

	/**
	 * Find all binary fields which make use of this binary.
	 *
	 * @return
	 */
	Result<HibBinaryField> findFields(HibBinary binary);
}
