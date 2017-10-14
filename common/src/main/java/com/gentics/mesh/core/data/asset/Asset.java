package com.gentics.mesh.core.data.asset;

import static com.gentics.mesh.Events.EVENT_ASSET_CREATED;
import static com.gentics.mesh.Events.EVENT_ASSET_DELETED;
import static com.gentics.mesh.Events.EVENT_ASSET_UPDATED;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.asset.AssetResponse;

/**
 * An Asset is a project specific element which represents a binary. Each asset also references an {@link AssetBinary} which holds the information where the
 * actual binary is stored. Multiple Assets can share the same {@link AssetBinary}. This information is decoupled in order to conserve disk space and also
 * handle cleanup operations.
 */
public interface Asset extends MeshCoreVertex<AssetResponse, Asset> {

	public static final String FILENAME_KEY = "filename";
	/**
	 * Type Value: {@value #TYPE}
	 */
	static final String TYPE = "asset";

	static final TypeInfo TYPE_INFO = new TypeInfo(TYPE, EVENT_ASSET_CREATED, EVENT_ASSET_UPDATED, EVENT_ASSET_DELETED);

	/**
	 * Return the filename of the asset.
	 * 
	 * @return
	 */
	default String getFilename() {
		return getProperty(FILENAME_KEY);
	}

	/**
	 * Set the filename for the asset.
	 * 
	 * @param filename
	 */
	default void setFilename(String filename) {
		setProperty(FILENAME_KEY, filename);
	}

	/**
	 * Return the asset binary which contains the binary information of this asset.
	 * 
	 * @return
	 */
	AssetBinary getAssetBinary();

	/**
	 * Set the asset binary of this asset.
	 * 
	 * @param assetBinary
	 */
	void setAssetBinary(AssetBinary assetBinary);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}
}
