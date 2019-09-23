package com.gentics.mesh.auth;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jwt.JWT;

public interface DynamicJWTAuth extends JWTAuth {

	JWT getJwt();

}
