package com.gentics.mesh.demo.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractCustomVerticle;
import com.gentics.mesh.demo.DemoDataProvider;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Dummy verticle that is used to setup basic demo data
 * 
 * @author johannes2
 *
 */
@Component
@Scope("singleton")
@SpringVerticle
public class CustomerVerticle extends AbstractCustomVerticle {

	private static Logger log = LoggerFactory.getLogger(CustomerVerticle.class);

	@Autowired
	private DemoDataProvider demoDataProvider;

	public CustomerVerticle() {
		super("test");
	}

	@Override
	public void registerEndPoints() throws Exception {
		// We only want to setup the demo data once
		if (BootstrapInitializer.isInitialSetup) {
			demoDataProvider.setup();
		} else {
			log.info("Demo graph was already setup once. Not invoking demo data setup.");
		}
	}
}
