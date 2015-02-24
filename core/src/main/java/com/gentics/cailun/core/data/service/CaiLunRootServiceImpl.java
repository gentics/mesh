package com.gentics.cailun.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.repository.CaiLunRootRepository;

@Component
@Transactional
public class CaiLunRootServiceImpl implements CaiLunRootService {

	@Autowired
	private CaiLunRootRepository rootRepository;

	@Override
	public CaiLunRoot findRoot() {
		return rootRepository.findRoot();
	}

	@Override
	public void save(CaiLunRoot rootNode) {
		rootRepository.save(rootNode);
	}
}
