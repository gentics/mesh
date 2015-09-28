package com.gentics.mesh.linkrenderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.link.LinkReplacer;
import com.gentics.mesh.core.link.LinkResolver;
import com.gentics.mesh.core.link.LinkResolverFactory;

public class LinkRendererTest extends AbstractBasicDBTest {

	final String content = "some bla START<a href=\"${Page(2)}\">Test</a>   dasasdg <a href=\"${Page(3)}\">Test</a>DEN";

	@Autowired
	private LinkResolverFactory<LinkResolver> resolverFactory;

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

		LinkReplacer<LinkResolver> replacer = new LinkReplacer(resolverFactory);
		replacer.replace("dgasd");
	}

	@Ignore("Disabled for now")
	@Test
	public void testRendering() throws IOException {
		System.out.println(content);
		int start = content.indexOf("#");
		int stop = content.lastIndexOf(")");
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
