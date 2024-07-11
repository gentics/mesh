package com.gentics.mesh.core.data;

/**
 * Binary data element.
 *
 * @author plyhun
 *
 */
public interface BinaryDataElement extends BaseElement {

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
	BinaryDataElement setSize(long sizeInBytes);

	/**
	 * Get the unique binary data identifier.
	 *
	 * @return
	 */
	Object getBinaryDataId();
}
