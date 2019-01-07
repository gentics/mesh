package com.gentics.mesh.madl;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import com.gentics.mesh.graphdb.spi.LegacyDatabase;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * SPI provider for Madl provider implementation.
 */
public class MadlService {

	private static final Logger log = LoggerFactory.getLogger(MadlService.class);
	private static MadlService service;
	private ServiceLoader<MadlProvider> loader;

	private MadlService() {
		loader = ServiceLoader.load(MadlProvider.class);
	}

	/**
	 * Return the database service instance.
	 * 
	 * @return
	 */
	public static synchronized MadlService getInstance() {
		if (service == null) {
			service = new MadlService();
		}
		return service;
	}

	/**
	 * Iterate over all providers return the last provider.
	 * 
	 * @return
	 */
	public MadlProvider getMadlProvider() {
		MadlProvider madl = null;
		try {
			Iterator<MadlProvider> madlProviders = loader.iterator();
			while (madl == null && madlProviders.hasNext()) {
				madl = madlProviders.next();
				log.debug("Found madl provider {" + madl.getClass() + "}");
			}
			if (madlProviders.hasNext()) {
				throw new RuntimeException("More than one provider was found. Please only include one provider.");
			}
		} catch (ServiceConfigurationError serviceError) {
			serviceError.printStackTrace();
		}
		if (madl == null) {
			throw new RuntimeException("Could not find madl provider.");
		}
		return madl;
	}

}
