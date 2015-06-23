package com.gentics.mesh.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public class AbstractMeshService {

	@Autowired
	protected I18NService i18n;

	@Autowired
	protected MeshSpringConfiguration springConfiguration;

	@Autowired
	protected FramedThreadedTransactionalGraph fg;

	@Autowired
	protected MeshSpringConfiguration configuration;

}
