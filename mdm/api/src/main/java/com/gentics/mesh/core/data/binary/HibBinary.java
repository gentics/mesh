package com.gentics.mesh.core.data.binary;

import java.io.InputStream;

import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.db.Supplier;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;

/**
 * Domain model for binaries.
 */
public interface HibBinary extends HibImageDataElement {

	/**
	 * Return the SHA512Sum of the binary.
	 *
	 * @return
	 */
	String getSHA512Sum();

	/**
	 * Set the SHA 512 Checksum
	 *
	 * @param sha512sum
	 * @return
	 */
	HibBinary setSHA512Sum(String sha512sum);

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
	HibBinary setCheckStatus(BinaryCheckStatus checkStatus);

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
	HibBinary setCheckSecret(String checkSecret);

	/**
	 * Opens a blocking {@link InputStream} to the binary file. This should only be used for some other blocking APIs (i.e. ImageIO)
	 *
	 * @return
	 */
	default Supplier<InputStream> openBlockingStream() {
		BinaryStorage storage = Tx.get().data().binaryStorage();
		String uuid = getUuid();
		return () -> storage.openBlockingStream(uuid);
	}

	@Override
	default Object getBinaryDataId() {
		return getSHA512Sum();
	}
}
