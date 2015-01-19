package com.gentics.vertx.cailun.shiro.spring;

import javax.annotation.PostConstruct;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.vertx.cailun.shiro.Neo4jAuthorizingRealm;

@Configuration
public class SecurityConfiguration {

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	@PostConstruct
	private void test() {
		System.out.println("POSTCONSTRUCT");
//		SecurityUtils.setSecurityManager(securityManager());
	}

	@Bean
	public Neo4jAuthorizingRealm customSecurityRealm() {
		return new Neo4jAuthorizingRealm();
	}

	public SessionsSecurityManager securityManager() {
		DefaultSecurityManager securityManager = new DefaultSecurityManager();
		securityManager.setRealm(customSecurityRealm());
		return securityManager;
	}

	@Bean
	public MethodInvokingFactoryBean methodInvokingFactoryBean() {
		MethodInvokingFactoryBean methodInvokingFactoryBean = new MethodInvokingFactoryBean();
		methodInvokingFactoryBean.setStaticMethod("org.apache.shiro.SecurityUtils.setSecurityManager");
		methodInvokingFactoryBean.setArguments(new Object[] { securityManager() });
		return methodInvokingFactoryBean;
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);
	}

}
