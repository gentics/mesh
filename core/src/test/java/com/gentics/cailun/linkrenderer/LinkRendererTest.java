package com.gentics.cailun.linkrenderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import javax.transaction.NotSupportedException;

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
import com.gentics.cailun.core.repository.generic.GenericContentRepository;
import com.gentics.cailun.core.rest.model.generic.GenericContent;
import com.gentics.cailun.core.rest.service.generic.GenericContentService;
import com.gentics.cailun.test.AbstractDBTest;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class LinkRendererTest extends AbstractDBTest {

	final String content = "some bla START<a href=\"${Page(2)}\">Test</a>   dasasdg <a href=\"${Page(3)}\">Test</a>DEN";

	@Autowired
	private CaiLunLinkResolverFactoryImpl<CaiLunLinkResolver> resolverFactory;

	@Autowired
	private GenericContentRepository contentRepository;

	@Autowired
	private GenericContentService contentService;

	@Test
	public void testNodeReplace() throws IOException, InterruptedException, ExecutionException, NotSupportedException {

		// Create some dummy content
		GenericContent content = new GenericContent();
		contentService.setName(content, german, "german name");
		contentService.setFilename(content, german, "german.html");
		contentRepository.save(content);

		GenericContent content2 = new GenericContent();
		contentService.setName(content2, english, "content 2 english");
		contentService.setFilename(content2, english, "english.html");
		contentRepository.save(content2);

		LinkReplacer<CaiLunLinkResolver> replacer = new LinkReplacer(resolverFactory);
		String out = replacer.replace("dgasd");
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
