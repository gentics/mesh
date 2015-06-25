package com.gentics.mesh.linkrenderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.link.LinkReplacer;
import com.gentics.mesh.core.link.LinkResolver;
import com.gentics.mesh.core.link.LinkResolverFactoryImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class LinkRendererTest extends AbstractDBTest {

	final String content = "some bla START<a href=\"${Page(2)}\">Test</a>   dasasdg <a href=\"${Page(3)}\">Test</a>DEN";

	@Autowired
	private LinkResolverFactoryImpl<LinkResolver> resolverFactory;

	@Autowired
	private MeshNodeService nodeService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testNodeReplace() throws IOException, InterruptedException, ExecutionException {

		Language german = data().getGerman();
		Language english = data().getEnglish();
		MeshNode parentNode = data().getFolder("2015");

		// Create some dummy content
		MeshNode content = parentNode.create();
		content.setI18NProperty(german, "displayName", "german name");
		content.setI18NProperty(german, "name", "german.html");

		MeshNode content2 = parentNode.create();
		content2.setI18NProperty(english, "displayName", "content 2 english");
		content2.setI18NProperty(english, "name", "english.html");

		LinkReplacer<LinkResolver> replacer = new LinkReplacer(resolverFactory);
		String out = replacer.replace("dgasd");
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
