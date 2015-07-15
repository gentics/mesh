package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.MeshAuthUser;

@Component
@Scope("singleton")
@SpringVerticle()
public class AuthenticationVerticle extends AbstractCoreApiVerticle {

	public AuthenticationVerticle() {
		super("auth");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		route("/me").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);
			transformAndResponde(rc, requestUser);
		});
		route("/login").method(GET).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			//			GenericResponse<String> response = new GenericResponse<>();
			//			response.setObject("OK");
			//			rc.response().end(toJson(response));
			});

	}
}
