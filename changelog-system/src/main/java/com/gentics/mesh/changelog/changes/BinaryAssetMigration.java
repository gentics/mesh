package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;

public class BinaryAssetMigration extends AbstractChange {

	@Override
	public String getName() {
		return "Binary Asset migration";
	}

	@Override
	public String getDescription() {
		return "Migrate all existing binary fields to assets within the asset management";
	}

	@Override
	public void apply() {
		// Find all binary field
		// Create a AssetBinary vertex per unique binary
		// Create the link between the asset and the binary field
		
		// Iterate over all found assets and only place the needed onces in the local filesystem folder.
	}

	@Override
	public String getUuid() {
		return "10668267FFDD4FE3A68267FFDD0FE389";
	}

}
