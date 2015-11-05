package com.gentics.mesh.demo.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractCustomVerticle;
import com.gentics.mesh.demo.TestDataProvider;
import com.gentics.mesh.rest.MeshRestClient;

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
	//private DemoDataProvider demoDataProvider;
	private TestDataProvider demoDataProvider;

	private MeshRestClient client;

	public CustomerVerticle() {
		super("test");
	}

	@Override
	public void registerEndPoints() throws Exception {
		if (BootstrapInitializer.isInitialSetup) {
			demoDataProvider.setup();
		} else {
			log.info("Demo graph was already setup once. Not invoking demo data setup.");
		}

		//		vertx.eventBus().consumer("mesh-startup-complete", mh -> {
		//
		//			client = MeshRestClient.create("localhost", config().getInteger("port"), vertx);
		//			client.setLogin("joe1", "test123");
		//			client.login();
		//
		//			Future<SchemaListResponse> schemasFuture = client.findSchemas(new PagingParameter(1, 100));
		//			schemasFuture.setHandler(rh -> {
		//				if (rh.failed()) {
		//					log.error("Could not load schemas", rh.cause());
		//				} else {
		//					SchemaListResponse list = rh.result();
		//					for (SchemaResponse schema : list.getData()) {
		//						log.info("Found schema {" + schema.getName() + "}");
		//						client.getClientSchemaStorage().addSchema(schema);
		//					}
		//				}
		//			});
		//			route("/*").handler(rc -> {
		//				log.info("Custom Verticle is handling a request");
		//				Future<NodeListResponse> fut = client.findNodes("dummy", new PagingParameter(1, 100));
		//
		//				fut.setHandler(rh -> {
		//					if (rh.failed()) {
		//						rc.fail(rh.cause());
		//					} else {
		//						NodeListResponse resp = rh.result();
		//						for (NodeResponse node : resp.getData()) {
		//							log.info("Found nodes {" + node.getUuid() + "}");
		//						}
		//					}
		//				});
		//				rc.response().end("test");
		//			});
		//		});

	}
}
