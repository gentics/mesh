package com.gentics.mesh.core.data;

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
}
