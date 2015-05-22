package com.gentics.mesh.demo.verticle;

import static com.gentics.mesh.core.data.model.auth.PermissionType.READ;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
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
		addPermissionTestHandler();
		try (Transaction tx = graphDb.beginTx()) {
			demoDataProvider.setup(1);
			tx.success();
		}
	}

	private void addPermissionTestHandler() {
		route("/permtest").method(GET).handler(rc -> {
			MeshNode content = nodeService.findOne(23L);
			rc.session().hasPermission(new MeshPermission(content, READ).toString(), handler -> {
				rc.response().end("User perm for node {" + content.getId() + "} : " + (handler.result() ? "jow" : "noe"));
			});
		});

	}

}
