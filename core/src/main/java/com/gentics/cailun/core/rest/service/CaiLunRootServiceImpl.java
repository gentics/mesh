package com.gentics.cailun.core.rest.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.CaiLunRootRepository;
import com.gentics.cailun.core.rest.model.CaiLunRoot;

public class CaiLunRootServiceImpl implements CaiLunRootService {

	@Autowired
	private CaiLunRootRepository rootRepository;

	@Override
	public CaiLunRoot findRoot() {
		return rootRepository.findRoot();
	}

}
