package com.gentics.mesh.demo.verticle;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractCustomVerticle;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.rest.MeshRestClient;

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

	private MeshRestClient client;

	public CustomerVerticle() {
		super("test");
	}

	@Override
	public void registerEndPoints() throws Exception {
		demoDataProvider.setup(1);

		vertx.eventBus().consumer("mesh-startup-complete", mh -> {

			client = new MeshRestClient("localhost", config().getInteger("port"));
			client.setLogin("joe1", "test123");
			client.login();

			Future<SchemaListResponse> schemasFuture = client.findSchemas(new PagingInfo(1, 100));
			schemasFuture.setHandler(rh -> {
				if (rh.failed()) {
					log.error("Could not load schemas", rh.cause());
				} else {
					SchemaListResponse list = rh.result();
					for (SchemaResponse schema : list.getData()) {
						System.out.println(schema.getName());
						client.getClientSchemaStorage().addSchema(schema);
					}
				}
			});
			route("/*").handler(rc -> {
				log.info("Custom Verticle is handling a request");
				Future<NodeListResponse> fut = client.findNodes("dummy", new PagingInfo(1, 100));

				fut.setHandler(rh -> {
					if (rh.failed()) {
						rc.fail(rh.cause());
					} else {
						NodeListResponse resp = rh.result();
						for (NodeResponse node : resp.getData()) {
							System.out.println(node.getUuid());
						}
					}
				});
				rc.response().end("test");
			});
		});

	}
}
