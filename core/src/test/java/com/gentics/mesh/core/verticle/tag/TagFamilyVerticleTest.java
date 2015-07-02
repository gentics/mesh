package com.gentics.mesh.core.verticle.tag;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.verticle.TagFamilyVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class TagFamilyVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagFamilyVerticle tagFamilyVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return tagFamilyVerticle;
	}

}
