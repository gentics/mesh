package com.gentics.mesh.security;

import org.springframework.security.crypto.password.PasswordEncoder;

public interface SecurityUtils {

	PasswordEncoder passwordEncoder();
}
