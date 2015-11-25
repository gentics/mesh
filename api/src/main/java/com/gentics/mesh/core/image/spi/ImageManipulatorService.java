package com.gentics.mesh.core.image.spi;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ImageManipulatorService {

	private static final Logger log = LoggerFactory.getLogger(ImageManipulatorService.class);

	private ServiceLoader<ImageManipulator> loader;
	private static ImageManipulatorService service;

	public static ImageManipulatorService getInstance() {
		if (service == null) {
			service = new ImageManipulatorService();
		}
		return service;
	}

	private ImageManipulatorService() {
		loader = ServiceLoader.load(ImageManipulator.class);
	}

	/**
	 * Iterate over all providers return the last provider.
	 * 
	 * @return
	 */
	public ImageManipulator getImageProvider() {
		ImageManipulator imageManipulator = null;
		//TODO fail when more than one provider was found?
		try {
			Iterator<ImageManipulator> providers = loader.iterator();
			while (imageManipulator == null && providers.hasNext()) {
				imageManipulator = providers.next();
				log.debug("Found service provider {" + imageManipulator.getClass() + "}");
			}
		} catch (ServiceConfigurationError serviceError) {
			serviceError.printStackTrace();
		}
		if (imageManipulator == null) {
			throw new RuntimeException("Could not find image provider.");
		}
		return imageManipulator;
	}

}