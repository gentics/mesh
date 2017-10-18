package com.gentics.mesh.core.data.asset;

import com.gentics.mesh.core.data.MeshVertex;

/**
 * Vertex which contains the actual information about the binary content that is needed by an {@link Asset}.
 */
public interface AssetBinary extends MeshVertex {

	public static final String SHA512SUM_KEY = "sha512sum";

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
	default AssetBinary setSHA512Sum(String sha512sum) {
		setProperty(SHA512SUM_KEY, sha512sum);
		return this;
	}

	/**
	 * Find all assets which make use of this asset binary.
	 * 
	 * @return
	 */
	Iterable<? extends Asset> findAssets();
}
