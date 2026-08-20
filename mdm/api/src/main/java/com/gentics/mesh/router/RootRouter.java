package com.gentics.mesh.router;

/**
 * The root router is the top level router of the routing stack.
 */
public interface RootRouter extends InternalRouter {

	/**
	 * Return the /api/v1 router
	 * 
	 * @return
	 */
	APIRouter apiRouter();

	/**
	 * Return the central router storage.
	 * 
	 * @return
	 */
	RouterStorage getStorage();

}
