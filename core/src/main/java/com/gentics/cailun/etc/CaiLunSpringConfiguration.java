package com.gentics.cailun.etc;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

import javax.annotation.PostConstruct;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.cailun.auth.CaiLunAuthServiceImpl;
import com.gentics.cailun.auth.EnhancedShiroAuthRealmImpl;
import com.gentics.cailun.auth.Neo4jAuthorizingRealm;
import com.gentics.cailun.core.RouterStorage;

@Configuration
public class CaiLunSpringConfiguration {

	private static final Logger log = LoggerFactory.getLogger(CaiLunSpringConfiguration.class);

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;
	
	@Autowired
	Neo4jSpringConfiguration rootConfig;

	@PostConstruct
	private void setup() {
		log.debug("Setting up {" + getClass().getCanonicalName() + "}");
	}

	@Bean
	public Vertx vertx() {
		VertxOptions options = new VertxOptions();
		// TODO remove debugging option
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
		Neo4jAuthorizingRealm realm = new Neo4jAuthorizingRealm();
		realm.setCacheManager(new MemoryConstrainedCacheManager());
		realm.setAuthenticationCachingEnabled(true);
		realm.setCachingEnabled(true);
		return realm;
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);
	}

	@Bean
	public RouterStorage routerStorage() {
		return new RouterStorage(vertx(), authService());
	}

}
