package com.gentics.vertx.cailun.starter;

import javax.inject.Inject;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.annotations.Optional;

import com.englishtown.vertx.jersey.impl.DefaultJerseyOptions;

/**
 * Enhanced version of the {@link DefaultJerseyOptions}. We need to add the spring DI context in order to enable jersey springdata.
 * 
 * @author johannes2
 *
 */
public class EnhancedJerseyOptions extends DefaultJerseyOptions {

	public static Object context;

	@Inject
	public EnhancedJerseyOptions(@Optional ServiceLocator locator) {
		super(locator);
	}

	@Override
	protected ResourceConfig getResourceConfig() {
		ResourceConfig rc = super.getResourceConfig();
		DynamicResourceHandler handler = new DynamicResourceHandler();
		handler.addDynamicResource(rc);
		if (context != null) {
			rc.property("contextConfig", context);
		}
		return rc;
	}

}
