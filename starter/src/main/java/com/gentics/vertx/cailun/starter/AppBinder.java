package com.gentics.vertx.cailun.starter;

import com.englishtown.vertx.hk2.HK2JerseyBinder;
import com.englishtown.vertx.jersey.JerseyOptions;

/**
 * HK2 Jersey binder
 */
public class AppBinder extends HK2JerseyBinder {

	@Override
	protected void configure() {
		bind(EnhancedJerseyOptions.class).to(JerseyOptions.class);
		super.configure();
	}

}
