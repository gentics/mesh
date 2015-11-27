package com.gentics.mesh.linkrenderer;

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
import com.gentics.mesh.util.UUIDUtil;

public class LinkRendererTest extends AbstractBasicDBTest {

	@Autowired
	private WebRootLinkReplacer replacer;

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

		String output = replacer.replace("dgasd");
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
		String output = replacer.replace(content);
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
