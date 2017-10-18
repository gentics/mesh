package com.gentics.mesh.storage;

import java.util.ServiceLoader;

import com.gentics.mesh.graphdb.DatabaseService;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BinaryStorageService {
	private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);
	private static BinaryStorageService service;
	private ServiceLoader<BinaryStorage> loader;

	private BinaryStorageService() {
		loader = ServiceLoader.load(BinaryStorage.class);
	}

	/**
	 * Return the database service instance.
	 * 
	 * @return
	 */
	public static synchronized BinaryStorageService getInstance() {
		if (service == null) {
			service = new BinaryStorageService();
		}
		return service;
	}
	
	
	public BinaryStorage getStorage() {
		return null;
	}
}
