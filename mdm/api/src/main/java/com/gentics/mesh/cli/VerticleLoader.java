package com.gentics.mesh.cli;

import java.util.List;

import io.reactivex.Completable;

/**
 * Serving Verticle loader 
 * 
 * @author plyhun
 *
 */
public interface VerticleLoader {

	/**
	 * Load verticles that are configured within the mesh configuration.
	 * 
	 * @param initialProjects
	 */
	Completable loadVerticles(List<String> initialProjects);

	/**
	 * Redeploy the serarch verticle.
	 * 
	 * @return
	 */
	Completable redeploySearchVerticle();
}