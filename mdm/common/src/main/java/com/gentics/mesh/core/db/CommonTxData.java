package com.gentics.mesh.core.db;

import com.gentics.mesh.router.RouterStorageRegistry;

/**
 * A developer extension API for {@link TxData}.
 * 
 * @author plyhun
 *
 */
public interface CommonTxData extends TxData {

	/**
	 * A router storage registry
	 * 
	 * @return
	 */
	RouterStorageRegistry routerStorageRegistry();
}
