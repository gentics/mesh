package com.gentics.vertx.cailun.auth;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthRealm;
import io.vertx.ext.auth.AuthService;
import io.vertx.ext.auth.ShiroAuthRealm;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityConfiguration {

	private static final transient Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	@PostConstruct
	private void setup() {
		log.debug("Setting up {" + getClass().getCanonicalName() + "}");
	}

	@Bean
	public Vertx vertx() {
		return Vertx.vertx();
	}

	@Bean
	public AuthService authService() {
		AuthRealm realm = ShiroAuthRealm.create(customSecurityRealm());
		AuthService authService = AuthService.createWithRealm(vertx(), realm, new JsonObject());
		return authService;
	}

	@Bean
	public Neo4jAuthorizingRealm customSecurityRealm() {
		return new Neo4jAuthorizingRealm();
	}

	//
	// public SessionsSecurityManager securityManager() {
	// DefaultSecurityManager securityManager = new DefaultSecurityManager();
	// securityManager.setRealm(customSecurityRealm());
	// return securityManager;
	// }
	//
	// @Bean
	// public MethodInvokingFactoryBean methodInvokingFactoryBean() {
	// MethodInvokingFactoryBean methodInvokingFactoryBean = new MethodInvokingFactoryBean();
	// methodInvokingFactoryBean.setStaticMethod("org.apache.shiro.SecurityUtils.setSecurityManager");
	// methodInvokingFactoryBean.setArguments(new Object[] { securityManager() });
	// return methodInvokingFactoryBean;
	// }

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);
	}

}
