package com.gentics.mesh.demo.verticle;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.demo.DemoDataProvider;

/**
 * Dummy verticle that is used to setup basic demo data
 * 
 * @author johannes2
 *
 */
@Component
@Scope("singleton")
@SpringVerticle
public class CustomerVerticle extends AbstractProjectRestVerticle {

	private static Logger log = LoggerFactory.getLogger(CustomerVerticle.class);

	@Autowired
	private DemoDataProvider demoDataProvider;

	public CustomerVerticle() {
		super("Content");
	}

	@Override
	public void registerEndPoints() throws Exception {
		demoDataProvider.setup(1);
	}

}
