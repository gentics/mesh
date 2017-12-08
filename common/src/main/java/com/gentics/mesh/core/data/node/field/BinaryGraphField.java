package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.rest.node.field.BinaryField;

/**
 * The BinaryField Domain Model interface. The field is an edge between the field container and the {@link Binary}
 */
public interface BinaryGraphField extends BasicGraphField<BinaryField>, MeshEdge {

	static final String BINARY_FILENAME_PROPERTY_KEY = "binaryFilename";

	static final String BINARY_SHA512SUM_PROPERTY_KEY = "binarySha512Sum";

	static final String BINARY_CONTENT_TYPE_PROPERTY_KEY = "binaryContentType";

	static final String BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY = "binaryImageDominantColor";

	/**
	 * Return the binary filename.
	 * 
	 * @return
	 */
	String getFileName();

	/**
	 * Copy the values of this field to the specified target field.
	 * 
	 * @param target
	 * @return Fluent API
	 */
	BinaryGraphField copyTo(BinaryGraphField target);

	/**
	 * Set the binary filename.
	 * 
	 * @param filenName
	 * @return Fluent API
	 */
	BinaryGraphField setFileName(String filenName);

	/**
	 * Return the binary mime type of the node.
	 * 
	 * @return
	 */
	String getMimeType();

	/**
	 * Set the binary mime type of the node.
	 * 
	 * @param mimeType
	 * @return Fluent API
	 */
	BinaryGraphField setMimeType(String mimeType);

	/**
	 * Check whether the binary data represents an image.
	 * 
	 * @return
	 */
	boolean hasImage();

	/**
	 * Set the binary image dominant color.
	 * 
	 * @param dominantColor
	 * @return Fluent API
	 */
	BinaryGraphField setImageDominantColor(String dominantColor);

	/**
	 * Return the binary image dominant color.
	 * 
	 * @return
	 */
	String getImageDominantColor();

	/**
	 * Return the uuid of the binary field.
	 * 
	 * @return
	 */
	String getUuid();

	/**
	 * Set the uuid of the binary field.
	 * 
	 * @param uuid
	 */
	void setUuid(String uuid);

	/**
	 * Return the referenced binary.
	 * 
	 * @return
	 */
	Binary getBinary();

}
