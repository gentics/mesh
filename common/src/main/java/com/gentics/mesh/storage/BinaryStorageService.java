package com.gentics.mesh.storage;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import com.gentics.mesh.madl.MadlService;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Service provider for binary storages.
 */
public class BinaryStorageService {
	private static final Logger log = LoggerFactory.getLogger(MadlService.class);
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

	/**
	 * Iterate over all providers and return the last provider.
	 * 
	 * @return
	 */
	public BinaryStorage getStorage() {
		BinaryStorage binaryStorage = null;
		// TODO fail when more than one provider was found?
		try {
			Iterator<BinaryStorage> providers = loader.iterator();
			while (binaryStorage == null && providers.hasNext()) {
				binaryStorage = providers.next();
				log.debug("Found service provider {" + binaryStorage.getClass() + "}");
			}
		} catch (ServiceConfigurationError serviceError) {
			serviceError.printStackTrace();
		}
		if (binaryStorage == null) {
			throw new RuntimeException("Could not find image provider.");
		}
		return binaryStorage;
	}
}