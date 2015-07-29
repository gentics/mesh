//package com.gentics.mesh.cli;
//
//import com.gentics.mesh.etc.config.MeshOptions;
//
//public interface Mesh {
//
//	/**
//	 * Creates the mesh instance using default options.
//	 *
//	 * @return the instance
//	 */
//	static Mesh vertx() {
//		return new MeshImpl();
//	}
//
//	/**
//	 * Creates the mesh instance using the specified options
//	 *
//	 * @param options
//	 *            the options to use
//	 * @return the instance
//	 */
//	static Mesh mesh(MeshOptions options) {
//		return new MeshImpl(options);
//	}
//
//	/**
//	 * Stop the the Mesh instance and release any resources held by it.
//	 * <p>
//	 * The instance cannot be used after it has been closed.
//	 */
//	void close();
//}
