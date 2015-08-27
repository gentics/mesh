package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.verticle.tag.ProjectTagVerticle;

public class TagSearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private ProjectTagVerticle tagVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(tagVerticle);
		return list;
	}

	@Test
	public void testDocumentCreation() {

	}

	@Test
	public void testDocumentUpdate() {

	}

	@Test
	public void testDocumentDeletion() {

	}

}
