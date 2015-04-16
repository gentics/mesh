package com.gentics.cailun.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.repository.CaiLunRootRepository;

@Component
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

	@Override
	public CaiLunRoot reload(CaiLunRoot rootNode) {
		return rootRepository.findOne(rootNode.getId());

	}
}
