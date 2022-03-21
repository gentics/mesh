package com.gentics.mesh.core.db;

import com.gentics.mesh.dagger.BaseMeshComponent;

/**
 * A developer extension API for {@link TxData}.
 * 
 * @author plyhun
 *
 */
public interface CommonTxData extends TxData {

	/**
	 * Root Mesh component access.
	 * 
	 * @return
	 */
	BaseMeshComponent mesh();
}
