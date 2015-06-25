package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.ProjectRoot;
import com.gentics.mesh.core.data.model.root.TagFamily;
import com.gentics.mesh.core.data.model.root.TagFamilyRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
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
		TagFamilyRoot tagFamilyRoot = project.create();
		TagFamily tagFamily = tagFamilyRoot.create("basic");

		Tag tag = tagFamily.create("dummyName");
		assertNotNull(tag);
		assertEquals("dummyName", tag.getName());
		tag.setName("renamed tag");

		Tag reloadedTag = tagService.findByUUID(tag.getUuid());
		assertNotNull(reloadedTag);
		assertNotNull(reloadedTag.getI18nProperties());
		assertEquals(1, reloadedTag.getI18nProperties().size());
		assertEquals("renamed tag", reloadedTag.getName());
	}
}
