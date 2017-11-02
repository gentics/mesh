package com.gentics.mesh.core.data.asset;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;

import io.vertx.core.buffer.Buffer;
import rx.Single;

/**
 * Vertex which contains the actual information about the binary content that is referenced by an {@link BinaryGraphField}.
 */
public interface Binary extends MeshVertex {

	public static final String SHA512SUM_KEY = "sha512sum";

	public static final String BINARY_FILESIZE_PROPERTY_KEY = "binaryFileSize";

	/**
	 * Return the binary data stream.
	 * 
	 * @return
	 */
	Single<Buffer> getStream();

	/**
	 * Return the sha512 checksum.
	 * 
	 * @return
	 */
	default String getSHA512Sum() {
		return getProperty(SHA512SUM_KEY);
	}

	/**
	 * Set the SHA512 checksum.
	 * 
	 * @param sha512sum
	 * @return
	 */
	default Binary setSHA512Sum(String sha512sum) {
		setProperty(SHA512SUM_KEY, sha512sum);
		return this;
	}

	/**
	 * Return the binary size in bytes.
	 * 
	 * @return
	 */
	default long getSize() {
		Long size = getProperty(BINARY_FILESIZE_PROPERTY_KEY);
		return size == null ? 0 : size;
	}

	/**
	 * Set the binary file size in bytes
	 * 
	 * @param sizeInBytes
	 * @return Fluent API
	 */
	default Binary setSize(long sizeInBytes) {
		setProperty(BINARY_FILESIZE_PROPERTY_KEY, sizeInBytes);
		return this;
	}

	/**
	 * Find all binary fields which make use of this binary.
	 * 
	 * @return
	 */
	Iterable<? extends BinaryGraphField> findAssets();
}
