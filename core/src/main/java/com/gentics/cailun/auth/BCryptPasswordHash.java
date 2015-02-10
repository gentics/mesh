package com.gentics.cailun.auth;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.etc.CaiLunSpringConfiguration;

/**
 * Wrapper for a bcrypted password hash. The wrapper is used within the {@link SimpleCredentialsMatcher} to validate the token password info.
 * 
 * @author johannes2
 *
 */
public class BCryptPasswordHash {

	private static final transient Logger log = LoggerFactory.getLogger(BCryptPasswordHash.class);

	private String accountPasswordHash;

	private CaiLunSpringConfiguration securityConfiguration;

	/**
	 * Create a new bcrypt password hash.
	 * 
	 * @param passwordHash
	 * @param securityConfig
	 */
	public BCryptPasswordHash(String passwordHash, CaiLunSpringConfiguration securityConfig) {
		// TODO inject securityConfiguration
		this.accountPasswordHash = passwordHash;
		this.securityConfiguration = securityConfig;
	}

	@Override
	public boolean equals(Object tokenPassword) {
		try {
			String tokenPasswordStr = String.valueOf((char[]) tokenPassword);
			if (StringUtils.isEmpty(accountPasswordHash) && tokenPasswordStr != null) {
				log.debug("The account password hash or token password string are invalid.");
				return false;
			}
			return securityConfiguration.passwordEncoder().matches(tokenPasswordStr, accountPasswordHash);
		} catch (ClassCastException e) {
			log.error("The given token password could not be transformed", e);
			return false;
		}

	}

}
