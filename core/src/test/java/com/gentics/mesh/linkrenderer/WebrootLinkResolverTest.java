package com.gentics.mesh.linkrenderer;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.util.UUIDUtil;

public class WebrootLinkResolverTest {

	@Test
	public void testResolving() throws InterruptedException, ExecutionException {
		String uuid = UUIDUtil.randomUUID();
		final String content = "some bla START<a href=\"{{mesh.link(\"" + uuid + ",\"en\"\")}}\">Test</a>   dasasdg <a href=\"{{mesh.link(\"" + uuid
				+ "\")}}\">Test</a>DEN";
		System.out.println("From: " + content);
		String output = new WebRootLinkReplacer().replace(content);
		System.out.println("To:   " + output);
	}
}
