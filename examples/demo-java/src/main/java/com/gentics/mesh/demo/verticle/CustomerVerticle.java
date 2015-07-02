package com.gentics.mesh.demo.verticle;

import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
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
		demoDataProvider.setup(1);
	}

	private void addPermissionTestHandler() {
		route("/permtest").method(GET).handler(rc -> {
			Node node = null; //nodeService.findOne(23L);
			MeshAuthUser requestUser = getUser(rc);
//			requestUser.hasPermission(content, READ_PERM), handler -> {
//				rc.response().end("User perm for node {" + content.getId() + "} : " + (handler.result() ? "jow" : "noe"));
//			});
		});

	}

}
