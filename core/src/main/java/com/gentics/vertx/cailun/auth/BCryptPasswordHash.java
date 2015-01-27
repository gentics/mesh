package com.gentics.vertx.cailun.auth;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.apache.shiro.codec.CodecSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Wrapper for a bcrypted password hash. The wrapper is used within the {@link SimpleCredentialsMatcher} to validate the token password info.
 * 
 * @author johannes2
 *
 */
public class BCryptPasswordHash {

	private static final transient Logger log = LoggerFactory.getLogger(BCryptPasswordHash.class);

	private String accountPasswordHash;
	
	private CaiLunConfiguration securityConfiguration;

	//TODO inject securityConfiguration
	public BCryptPasswordHash(String passwordHash, CaiLunConfiguration securityConfig) {
		this.accountPasswordHash = passwordHash;
		this.securityConfiguration = securityConfig;
	}

	@Override
	public boolean equals(Object tokenPassword) {
		String tokenPasswordString = null;
		try {
			tokenPasswordString = String.valueOf((char[]) tokenPassword);
		} catch (ClassCastException e) {
			log.error("The given token password could not be transformed", e);
			return false;
		}
		if (StringUtils.isEmpty(accountPasswordHash) && tokenPasswordString != null) {
			log.debug("The account password hash or token password string are invalid.");
			return false;
		}
		return securityConfiguration.passwordEncoder().matches(tokenPasswordString, accountPasswordHash);
	}

}
