package com.gentics.mesh.auth;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Wrapper for a bcrypted password hash.
 */
public class BCryptPasswordHash {

	private static final transient Logger log = LoggerFactory.getLogger(BCryptPasswordHash.class);

	private String accountPasswordHash;

	private BCryptPasswordEncoder passwordEncoder;

	/**
	 * Create a new bcrypt password hash.
	 * 
	 * @param passwordHash
	 *            Hashed password
	 * @param passwordEncoder
	 *            Password encoder
	 */
	public BCryptPasswordHash(String passwordHash, BCryptPasswordEncoder passwordEncoder) {
		// TODO inject securityConfiguration
		this.accountPasswordHash = passwordHash;
		this.passwordEncoder = passwordEncoder;
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
			return passwordEncoder.matches(tokenPasswordStr, accountPasswordHash);
		} catch (ClassCastException e) {
			if (log.isErrorEnabled()) {
				log.error("The given token password could not be transformed", e);
			}
			return false;
		}

	}

}
