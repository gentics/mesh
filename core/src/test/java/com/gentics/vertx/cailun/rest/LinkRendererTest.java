package com.gentics.vertx.cailun.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.gentics.vertx.cailun.core.CaiLunLinkResolver;
import com.gentics.vertx.cailun.core.CaiLunLinkResolverFactoryImpl;
import com.gentics.vertx.cailun.core.LinkReplacer;
import com.gentics.vertx.cailun.core.LinkResolverFactory;

public class LinkRendererTest {
	String content = "some bla START<a href=\"${Page(2)}\">Test</a> - <a href=\"${Page(3)}\">Test</a>ENDENDEN- <a href=\"${Page(4)}\">Test</a>ENDENDEN";

	@Test
	public void testNodeReplace() throws IOException, InterruptedException, ExecutionException {
		String allContent = "";
		for (int i = 0; i < 100; i++) {
			allContent += content;
		}
		LinkResolverFactory factory = new CaiLunLinkResolverFactoryImpl<CaiLunLinkResolver>();
		LinkReplacer<CaiLunLinkResolver> replacer = new LinkReplacer(factory);
//		for (int i = 0; i < 2000; i++) {
			// System.out.println(allContent);
			String out = replacer.replace(allContent);
			 System.out.println(out);
//		}
	}
//
//	@Test
//	public void testPLinkReplacer2() throws IOException {
//		String allContent = "";
//		for (int i = 0; i < 100; i++) {
//			allContent += content;
//		}
//		for (int i = 0; i < 20000; i++) {
//			PLinkInputStream pl = new PLinkInputStream(IOUtils.toInputStream(allContent));
//			String out = IOUtils.toString(pl);
//			// System.out.println(out);
//		}
//	}
//
//	@Test
//	public void testPLinkReplacer() throws IOException {
//		String allContent = "";
//		for (int i = 0; i < 100; i++) {
//			allContent += content;
//		}
//		ByteArrayOutputStream bos = new ByteArrayOutputStream(allContent.length() * 2000);
//		PLinkOutputStream os = new PLinkOutputStream(bos);
//		for (int i = 0; i < 20000; i++) {
//			os.write(allContent.getBytes());
//		}
//		os.flush();
//	}

	private String getUrlForPage(Long id) {
		return "/var/www/index.html";
	}

//	@Test
//	public void testStringReplace() throws InterruptedException {
//		String allContent = "";
//		for (int i = 0; i < 100; i++) {
//			allContent += content;
//		}
//		for (int i = 0; i < 2000; i++) {
//
//			// int s = 0;
//			// while (s != -1) {
//			// s = content.indexOf("${", s + 1);
//			// int e = content.indexOf("}", s);
//			// //System.out.println(content.substring(s,e));
//			// }
//			for (int e = 0; e < 300; e++) {
//				String out = StringUtils.replace(allContent, "${Page(2)}", "atom.xml");
//			}
//		}
//		// System.out.println(out);
//	}

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
