package com.gentics.cailun.demo.verticle;

import static com.gentics.cailun.core.data.model.auth.PermissionType.READ;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Session;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.demo.DemoDataProvider;

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
		demoDataProvider.setup(3);
	}

	private void addPermissionTestHandler() {
		route("/permtest").method(GET).handler(rh -> {
			Session session = rh.session();
			Content content = contentService.findOne(23L);
			boolean perm = getAuthService().hasPermission(session.getLoginID(), new CaiLunPermission(content, READ));
			rh.response().end("User perm for node {" + content.getId() + "} : " + (perm ? "jow" : "noe"));
		});

	}

}
