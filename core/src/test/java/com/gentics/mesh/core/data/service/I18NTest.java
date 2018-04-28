package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.util.Locale;
import java.util.MissingResourceException;

import org.junit.Test;

import com.gentics.mesh.context.AbstractActionContext;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class I18NTest extends AbstractMeshTest {

	@Test
	public void testGerman() {
		Locale locale = new Locale("de", "DE");
		assertEquals("Fehler", I18NUtil.get(locale, "error"));
	}

	@Test
	public void testEnglish() {
		Locale locale = new Locale("en", "US");
		assertEquals("Error", I18NUtil.get(locale, "error"));
	}

	@Test
	public void testFallback() {
		Locale locale = new Locale("jp");
		assertEquals("Error", I18NUtil.get(locale, "error"));
	}

	@Test(expected = MissingResourceException.class)
	public void testWrongI18NKey() {
		Locale locale = new Locale("jp");
		I18NUtil.get(locale, "fadsfgasdgasdg");
	}

	@Test
	public void testWrongI18NKey2() {
		Locale locale = new Locale("jp");
		String bogusKey = "fadsfgasdgasdg";
		assertEquals(bogusKey, I18NUtil.get(locale, bogusKey, "test"));
	}

	@Test
	public void testFormattedMessage() {
		Locale locale = new Locale("de", "DE");
		assertEquals("Gruppe konnte nicht gefunden werden: \"testgroup\"", I18NUtil.get(locale, "group_not_found", new String[] { "testgroup" }));
	}

	@Test
	public void testLocaleFromHeader() {
		Locale locale = AbstractActionContext.getLocale("da, en-gb;q=0.8, en;q=0.7, de;q=0.81");
		assertEquals("de", locale.getLanguage());
		locale = AbstractActionContext.getLocale("da, en-gb;q=0.9, en;q=0.7, de;q=0.81");
		assertEquals("en", locale.getLanguage());
		locale = AbstractActionContext.getLocale("de, en-gb;q=0.9, en;q=0.7, de;q=0.81");
		assertEquals("de", locale.getLanguage());
	}
}
