package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;

/**
 * Binary data element.
 *
 * @author plyhun
 *
 */
public interface HibBinaryDataElement extends HibBaseElement {

	/**
	 * Return the size of the binary data.
	 *
	 * @return
	 */
	long getSize();

	/**
	 * Set the size of the binary data
	 *
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	HibBinaryDataElement setSize(long sizeInBytes);

	/**
	 * Get the unique binary data identifier.
	 *
	 * @return
	 */
	Object getBinaryDataId();

	/**
	 * Return the check status of the binary (one of ACCEPTED, DENIED or POSTPONED).
	 * @return The check status of the binary.
	 */
	BinaryCheckStatus getCheckStatus();

	/**
	 * Set the check status of the binary (one of ACCEPTED, DENIED or POSTPONDED).
	 * @param checkStatus The check status to set.
	 * @return Fluent API.
	 */
	<T extends HibBinaryDataElement> T setCheckStatus(BinaryCheckStatus checkStatus);

	/**
	 * Return the check secret of the binary.
	 * @return The check secret of the binary.
	 */
	String getCheckSecret();

	/**
	 * Set the check secret of the binary.
	 * @param checkSecret The binaries check secret.
	 * @return Fluent API.
	 */
	<T extends HibBinaryDataElement> T setCheckSecret(String checkSecret);
}
