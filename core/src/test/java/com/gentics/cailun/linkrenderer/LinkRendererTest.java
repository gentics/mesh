package com.gentics.cailun.linkrenderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.link.CaiLunLinkResolver;
import com.gentics.cailun.core.link.CaiLunLinkResolverFactoryImpl;
import com.gentics.cailun.core.link.LinkReplacer;
import com.gentics.cailun.core.repository.GlobalContentRepository;
import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.LocalizedContent;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class LinkRendererTest {

	final String content = "some bla START<a href=\"${Page(2)}\">Test</a>   dasasdg <a href=\"${Page(3)}\">Test</a>DEN";

	@Autowired
	CaiLunLinkResolverFactoryImpl<CaiLunLinkResolver> resolverFactory;

	@Autowired
	GlobalContentRepository pageRepository;

	@Test
	public void testNodeReplace() throws IOException, InterruptedException, ExecutionException {

		// Create some dummy content
		LocalizedContent content = new LocalizedContent();
		content.addContent(new Content("test content"));
		pageRepository.save(content);
		Content page2 = new Content();
//		page2.setContent(content);
		pageRepository.save(page2);

		LinkReplacer<CaiLunLinkResolver> replacer = new LinkReplacer(resolverFactory);
		String out = replacer.replace(content);
		System.out.println(out);
	}

	// @Test
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
