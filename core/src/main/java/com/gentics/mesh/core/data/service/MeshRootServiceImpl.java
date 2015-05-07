package com.gentics.mesh.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.mesh.core.data.model.MeshRoot;
import com.gentics.mesh.core.repository.MeshRootRepository;

@Component
@Transactional(readOnly = true)
public class MeshRootServiceImpl implements MeshRootService {

	@Autowired
	private MeshRootRepository rootRepository;

	@Override
	public MeshRoot findRoot() {
		return rootRepository.findRoot();
	}

	@Override
	public void save(MeshRoot rootNode) {
		rootRepository.save(rootNode);
	}

	@Override
	public MeshRoot reload(MeshRoot rootNode) {
		return rootRepository.findOne(rootNode.getId());

	}
}
