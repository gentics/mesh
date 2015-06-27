package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.impl.TagImpl;
import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.ProjectRoot;
import com.gentics.mesh.core.data.model.root.TagFamilyRoot;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.MeshRootService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.test.AbstractDBTest;

public class AtomicTagTest extends AbstractDBTest {

	@Autowired
	private TagService tagService;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private MeshRootService rootService;

	@Test
	public void testTagCreation() {
		MeshRoot meshRoot = rootService.create();
		LanguageRoot languageRoot = meshRoot.createLanguageRoot();
		assertNotNull(languageRoot);
		Language language = languageRoot.create("Deutsch", "de");
		Language english = languageRoot.create("English", "en");

		ProjectRoot projectRoot = meshRoot.createProjectRoot();
		Project project = projectRoot.create("dummy");
		TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
		TagFamily tagFamily = tagFamilyRoot.create("basic");

		Tag tag = tagFamily.create("dummyName");
		String uuid = tag.getUuid();
		assertNotNull(tag);
		assertEquals("dummyName", tag.getName());
		tag.setName("renamed tag");
		assertEquals("renamed tag", tag.getName());

		Tag reloadedTag = tagService.findByUUID(uuid);
		assertNotNull(reloadedTag);
		assertNotNull(reloadedTag.getFieldContainers());
		assertEquals(1, reloadedTag.getFieldContainers().size());
		assertEquals("renamed tag", reloadedTag.getName());
	}
}
