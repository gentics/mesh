package com.gentics.mesh.auth;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.etc.MeshSpringConfiguration;

/**
 * Wrapper for a bcrypted password hash.
 */
public class BCryptPasswordHash {

	private static final transient Logger log = LoggerFactory.getLogger(BCryptPasswordHash.class);

	private String accountPasswordHash;

	private MeshSpringConfiguration springConfiguration;

	/**
	 * Create a new bcrypt password hash.
	 * 
	 * @param passwordHash
	 * @param securityConfig
	 */
	public BCryptPasswordHash(String passwordHash, MeshSpringConfiguration securityConfig) {
		// TODO inject securityConfiguration
		this.accountPasswordHash = passwordHash;
		this.springConfiguration = securityConfig;
	}

	@Override
	public boolean equals(Object tokenPassword) {
		try {
			String tokenPasswordStr = String.valueOf((char[]) tokenPassword);
			if (StringUtils.isEmpty(accountPasswordHash) && tokenPasswordStr != null) {
				if (log.isDebugEnabled()) {
					log.debug("The account password hash or token password string are invalid.");
				}
				return false;
			}
			return springConfiguration.passwordEncoder().matches(tokenPasswordStr, accountPasswordHash);
		} catch (ClassCastException e) {
			if (log.isErrorEnabled()) {
				log.error("The given token password could not be transformed", e);
			}
			return false;
		}

	}

}
