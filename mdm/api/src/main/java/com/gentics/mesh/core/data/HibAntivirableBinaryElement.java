package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;

/**
 * A binary data element, that is a subject of an external virus check.
 * 
 * @author plyhun
 *
 */
public interface HibAntivirableBinaryElement extends HibBinaryDataElement {

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
	HibAntivirableBinaryElement setCheckStatus(BinaryCheckStatus checkStatus);

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
	HibAntivirableBinaryElement setCheckSecret(String checkSecret);
}
