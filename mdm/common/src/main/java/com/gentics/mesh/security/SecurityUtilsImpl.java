package com.gentics.mesh.security;

import javax.inject.Inject;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * General security utils implementation.
 * 
 * @author plyhun
 *
 */
public class SecurityUtilsImpl implements SecurityUtils {
	
	private final PasswordEncoder passwordEncoder;

	@Inject
	public SecurityUtilsImpl(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public PasswordEncoder passwordEncoder() {
		return passwordEncoder;
	}

}
