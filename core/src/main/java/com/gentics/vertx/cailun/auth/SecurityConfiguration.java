package com.gentics.vertx.cailun.auth;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import javax.annotation.PostConstruct;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityConfiguration {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	@PostConstruct
	private void setup() {
		log.debug("Setting up {" + getClass().getCanonicalName() + "}");
	}

	@Bean
	public Vertx vertx() {
		VertxOptions options = new VertxOptions();
		//TODO remove debugging option
		options.setBlockedThreadCheckPeriod(Long.MAX_VALUE);
		return Vertx.vertx(options);
	}

	@Bean
	public CaiLunAuthServiceImpl authService() {
		EnhancedShiroAuthRealmImpl realm = new EnhancedShiroAuthRealmImpl(customSecurityRealm());
		CaiLunAuthServiceImpl authService = new CaiLunAuthServiceImpl(vertx(), realm, new JsonObject());
		SecurityUtils.setSecurityManager(realm.getSecurityManager());
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
