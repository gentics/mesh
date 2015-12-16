package com.gentics.mesh.linkrenderer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.link.WebRootLinkReplacer.Type;
import com.gentics.mesh.util.UUIDUtil;

public class LinkRendererTest extends AbstractBasicDBTest {

	@Autowired
	private WebRootLinkReplacer replacer;

	@Test
	public void testLinkReplacerTypeOff() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link('" + uuid + "')}}";
		String replacedContent = replacer.replace(content, Type.OFF);

		assertEquals("Check rendered content", content, replacedContent);
	}

	@Test
	public void testLinkReplacerTypeShort() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link('" + uuid + "')}}";
		String replacedContent = replacer.replace(content, Type.SHORT);

		assertEquals("Check rendered content", "/News/News+Overview.en.html", replacedContent);
	}

	@Test
	public void testLinkReplacerTypeMedium() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link('" + uuid + "')}}";
		String replacedContent = replacer.replace(content, Type.MEDIUM);

		assertEquals("Check rendered content", "/dummy/News/News+Overview.en.html", replacedContent);
	}

	@Test
	public void testLinkReplacerTypeFull() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link('" + uuid + "')}}";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content", "/api/v1/dummy/webroot/News/News+Overview.en.html", replacedContent);
	}

	@Test
	public void testLinkAtStart() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link('" + uuid + "')}} postfix";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content", "/api/v1/dummy/webroot/News/News+Overview.en.html postfix", replacedContent);
	}

	@Test
	public void testLinkAtEnd() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "prefix {{mesh.link('" + uuid + "')}}";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content", "prefix /api/v1/dummy/webroot/News/News+Overview.en.html", replacedContent);
	}

	@Test
	public void testLinkInMiddle() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "prefix {{mesh.link('" + uuid + "')}} postfix";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content", "prefix /api/v1/dummy/webroot/News/News+Overview.en.html postfix", replacedContent);
	}

	@Test
	public void testAdjacentLinks() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link('" + uuid + "')}}{{mesh.link('" + uuid + "')}}";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content",
				"/api/v1/dummy/webroot/News/News+Overview.en.html/api/v1/dummy/webroot/News/News+Overview.en.html",
				replacedContent);
	}

	@Test
	public void testNonAdjacentLinks() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link('" + uuid + "')}} in between {{mesh.link('" + uuid + "')}}";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content",
				"/api/v1/dummy/webroot/News/News+Overview.en.html in between /api/v1/dummy/webroot/News/News+Overview.en.html",
				replacedContent);
	}

	@Test
	public void testInvalidLinks() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link('" + uuid + "')}";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content", content, replacedContent);
	}

	@Test
	public void testSingleQuote() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "'\"{{mesh.link('" + uuid + "')}}\"'";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content", "'\"/api/v1/dummy/webroot/News/News+Overview.en.html\"'", replacedContent);
	}

	@Test
	public void testDoubleQuote() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "'\"{{mesh.link(\"" + uuid + "\")}}\"'";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content", "'\"/api/v1/dummy/webroot/News/News+Overview.en.html\"'", replacedContent);
	}

	@Test
	public void testGerman() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link(\"" + uuid + "\", \"de\")}}";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content", "/api/v1/dummy/webroot/Neuigkeiten/News+Overview.de.html", replacedContent);
	}

	@Test
	public void testEnglish() {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "{{mesh.link(\"" + uuid + "\", \"en\")}}";
		String replacedContent = replacer.replace(content, Type.FULL);

		assertEquals("Check rendered content", "/api/v1/dummy/webroot/News/News+Overview.en.html", replacedContent);
	}

	@Test
	public void testNodeReplace() throws IOException, InterruptedException, ExecutionException {
		Language german = german();
		Language english = english();
		Node parentNode = folder("2015");

		// Create some dummy content
		Node content = parentNode.create(user(), schemaContainer("content"), project());
		NodeGraphFieldContainer germanContainer = content.getOrCreateGraphFieldContainer(german);
		germanContainer.createString("displayName").setString("german name");
		germanContainer.createString("name").setString("german.html");

		Node content2 = parentNode.create(user(), schemaContainer("content"), project());
		NodeGraphFieldContainer englishContainer = content2.getOrCreateGraphFieldContainer(english);
		englishContainer.createString("displayName").setString("content 2 english");
		englishContainer.createString("name").setString("english.html");

		String output = replacer.replace("dgasd", null);
	}

	@Test
	public void testBinaryFieldLinkResolving() {
		
	}
	
	@Test
	public void testResolving() throws InterruptedException, ExecutionException {
		Node newsNode = content("news overview");
		String uuid = newsNode.getUuid();
		final String content = "some bla START<a href=\"{{mesh.link('" + uuid + "','en')}}\">Test</a>   dasasdg <a href=\"{{mesh.link(\"" + uuid
				+ "\")}}\">Test</a>DEN";
		System.out.println("From: " + content);
		String output = replacer.replace(content, null);
		System.out.println("To:   " + output);
	}

	@Test
	public void testRendering() throws IOException {
		String uuid = UUIDUtil.randomUUID();
		final String content = "some bla START<a href=\"{{mesh.link(\"" + uuid + "\")}}\">Test</a>   dasasdg <a href=\"{{mesh.link(\"" + uuid
				+ "\")}}\">Test</a>DEN";
		System.out.println(content);
		int start = content.indexOf("{{mesh.link(");
		int stop = content.lastIndexOf(")}}") + 3;
		int len = stop - start;
		System.out.println("from " + start + " to " + stop + " len " + len);
		InputStream in = IOUtils.toInputStream(content);
		try {
			int e = 0;
			for (int c; (c = in.read()) != -1 /* EOF */;) {
				if (e >= start && e < stop) {
					// System.out.println("skipping");
					in.skip(len);
					System.out.print("Hugsdibugsdi");
					e = stop;
				} else {
					System.out.print((char) c);
				}
				e++;
			}
		} finally {
			in.close();
		}
	}

}
