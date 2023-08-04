package com.gentics.mesh.core.data.binary;

import java.io.InputStream;

import com.gentics.mesh.core.data.HibAntivirableBinaryElement;
import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.db.Supplier;
import com.gentics.mesh.core.db.Tx;

/**
 * Domain model for binaries.
 */
public interface HibBinary extends HibImageDataElement, HibAntivirableBinaryElement {

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
