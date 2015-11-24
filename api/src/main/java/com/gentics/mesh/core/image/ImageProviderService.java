package com.gentics.mesh.core.image;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ImageProviderService {

	private static final Logger log = LoggerFactory.getLogger(ImageProviderService.class);

	private ServiceLoader<ImageProvider> loader;
	private static ImageProviderService service;

	public static ImageProviderService getInstance() {
		if (service == null) {
			service = new ImageProviderService();
		}
		return service;
	}

	private ImageProviderService() {
		loader = ServiceLoader.load(ImageProvider.class);
	}

	/**
	 * Iterate over all providers return the last provider.
	 * 
	 * @return
	 */
	public ImageProvider getImageProvider() {
		ImageProvider provider = null;
		//TODO fail when more than one provider was found?
		try {
			Iterator<ImageProvider> providers = loader.iterator();
			while (provider == null && providers.hasNext()) {
				provider = providers.next();
				log.debug("Found service provider {" + provider.getClass() + "}");
			}
		} catch (ServiceConfigurationError serviceError) {
			serviceError.printStackTrace();
		}
		return provider;
	}

}