package com.gentics.mesh.core.verticle;

import static io.vertx.core.http.HttpMethod.GET;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.apache.http.entity.ContentType;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;

@Component
@Scope("singleton")
@SpringVerticle()
public class AuthenticationVerticle extends AbstractCoreApiVerticle {

	@Context
	SecurityContext securityContext;

	public AuthenticationVerticle() {
		super("auth");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addLoginHandler();
		addUserInfoHandler();
	}

	private void addUserInfoHandler() {
		route("/principal").method(GET).handler(rc -> {
			rc.response().end(securityContext.getUserPrincipal().getName());
		});
	}

	private void addLoginHandler() {
		route("/login").consumes(ContentType.APPLICATION_JSON.getMimeType()).method(GET).handler(rc -> {
//			GenericResponse<String> response = new GenericResponse<>();
//			response.setObject("OK");
//			rc.response().end(toJson(response));
		});

	}

}
