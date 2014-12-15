package com.gentics.vertx.cailun.starter;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.gentics.vertx.cailun.model.perm.User;
import com.gentics.vertx.cailun.repository.UserRepository;

public class FakeUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User person = userRepository.findByFirstnameEquals(username);
		if (person == null) {
			throw new UsernameNotFoundException("Username " + username + " not found");
		}
		return new org.springframework.security.core.userdetails.User(username, "password", getGrantedAuthorities(username));
	}

	private Collection<? extends GrantedAuthority> getGrantedAuthorities(String username) {
		Collection<? extends GrantedAuthority> authorities;
		if (username.equals("John")) {
			authorities = Arrays.asList(() -> "ROLE_ADMIN", () -> "ROLE_BASIC");
		} else {
			authorities = Arrays.asList(() -> "ROLE_BASIC");
		}
		return authorities;
	}

}
