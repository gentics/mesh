package com.gentics.cailun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;

public class GroupRepositoryImpl {
	
	public Page<Group> findAll(User requestUser, Pageable pageable) {
		return null;
	}
}
