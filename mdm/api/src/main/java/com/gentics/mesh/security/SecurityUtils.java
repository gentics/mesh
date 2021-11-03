package com.gentics.mesh.security;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A collection of security related entities and methods.
 * 
 * @author plyhun
 *
 */
public interface SecurityUtils {

	/**
	 * Get an implementation of {@link PasswordEncoder}.
	 * 
	 * @return
	 */
	PasswordEncoder passwordEncoder();
}
