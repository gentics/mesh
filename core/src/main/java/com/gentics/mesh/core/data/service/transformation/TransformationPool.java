package com.gentics.mesh.core.data.service.transformation;

import java.util.concurrent.ForkJoinPool;

public class TransformationPool {

	//TODO make poolsize configurable
	private static ForkJoinPool pool = new ForkJoinPool(8);

	public static ForkJoinPool getPool() {
		return pool;
	}
}
