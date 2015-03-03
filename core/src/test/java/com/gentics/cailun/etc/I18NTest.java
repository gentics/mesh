package com.gentics.cailun.etc;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.service.I18NService;
import com.gentics.cailun.test.AbstractDBTest;

public class I18NTest extends AbstractDBTest {

	@Autowired
	private I18NService i18n;

	@Test
	public void testGerman() {
		Locale locale = new Locale("de", "DE");
		assertEquals("Fehler", i18n.get(locale, "error"));
	}

	@Test
	public void testEnglish() {
		Locale locale = new Locale("en", "US");
		assertEquals("Error", i18n.get(locale, "error"));
	}

	@Test
	public void testFallback() {
		Locale locale = new Locale("jp");
		assertEquals("Error", i18n.get(locale, "error"));
	}

	@Test
	public void testFormattedMessage() {
		Locale locale = new Locale("de", "DE");
		assertEquals("Gruppe konnte nicht gefunden werden: \"testgroup\"", i18n.get(locale, "group_not_found", new String[] { "testgroup" }));
	}
}
