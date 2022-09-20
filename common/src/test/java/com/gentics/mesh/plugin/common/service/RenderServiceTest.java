package com.gentics.mesh.plugin.common.service;

import com.gentics.mesh.plugin.common.TemplateWrapper;
import com.github.jknack.handlebars.HandlebarsException;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.vertx.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test template path adjustment and rendering (RxJava and callback versions).
 */
public class RenderServiceTest {

	private static final String[] TEMPLATES = { "template-1.hbs", "template-2.hbs", "non-existent-template.hbs" };

	private static Vertx vertx;
	private static String templateBasePath;
	private static RenderService renderService;

	@BeforeClass
	public static void setupOnce() throws URISyntaxException {
		vertx = Vertx.vertx();
		templateBasePath = new File(RenderServiceTest.class.getResource("/templates").toURI()).getPath();

		io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine delegateEngine = io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine.create(vertx);

		renderService = new RenderService(templateBasePath, new HandlebarsTemplateEngine(delegateEngine));
	}

	@AfterClass
	public static void tearDown() {
		vertx.close();
	}

	@Test
	public void testAdjustTemplatePath() {
		String template = "/my-templates/foo.hbs";
		String expected = templateBasePath + template;

		assertThat(renderService.adjustTemplatePath(expected))
			.as("Adjusted correct path")
			.isEqualTo(expected);

		assertThat(renderService.adjustTemplatePath(template))
			.as("Adjusted relative path")
			.isEqualTo(expected);
	}

	@Test
	public void testRenderWithPath() {
		doTestRenderWithPath(1, TEMPLATES[0], true);
		doTestRenderWithPath(2, TEMPLATES[1], true);
	}

	@Test
	public void testRenderWithInvalidPath() {
		doTestRenderWithPath(0, TEMPLATES[2], false);
	}

	@Test
	public void testRenderWithSource() {
		doTestRender("<inline>", "Template file <inline>: none", "none", true);
	}

	@Test
	public void testRenderWithInvalidSource() {
		doTestRender(null, "Invalid template {{", null, false);
	}

	public void testRxRenderWithPath() throws InterruptedException {
		doTestRxRenderWithPath(1, TEMPLATES[0], null);
		doTestRxRenderWithPath(2, TEMPLATES[1], null);
	}

	@Test
	public void testRxRenderWithInvalidPath() throws InterruptedException {
		doTestRxRenderWithPath(0, TEMPLATES[2], IllegalArgumentException.class);
	}

	@Test
	public void testRxRenderWithSource() throws InterruptedException {
		doTestRxRender("<inline>", "Template file <inline>: none", "none", null);
	}

	@Test
	public void testRxRenderWithInvalidSource() throws InterruptedException {
		doTestRxRender(null, "Invalid template {{", null, HandlebarsException.class);
	}

	private void doTestRenderWithPath(int fileNr, String templatePath, boolean expectSuccess) {
		doTestRender(String.valueOf(fileNr), null, templatePath, expectSuccess);
	}

	private void doTestRender(String fileId, String templateSource, String templatePath, boolean expectSuccess) {
		Map<String, Object> ctx = Collections.singletonMap("file", templatePath);
		TemplateWrapper template = TemplateWrapper.wrap(templateSource, ArrayUtils.add(TEMPLATES, 0, templatePath));

		renderService.render(ctx, template, rs -> {
			if (expectSuccess) {
				assertThat(rs.succeeded())
					.as("Rendering succeeded")
					.isTrue();

				assertThat(rs.result().toString().trim())
					.as("Render result")
					.isEqualTo(String.format("Template file %s: %s", fileId, templatePath));
			} else {
				assertThat(rs.succeeded())
					.as("Rendering failed")
					.isFalse();
			}
		});
	}

	private void doTestRxRenderWithPath(int fileNr, String templatePath, Class<? extends Throwable> expectedError) throws InterruptedException {
		doTestRxRender(String.valueOf(fileNr), null, templatePath, expectedError);
	}

	private void doTestRxRender(String fileId, String templateSource, String templatePath, Class<? extends Throwable> expectedError) throws InterruptedException {
		Map<String, Object> ctx = Collections.singletonMap("file", templatePath);
		TemplateWrapper template = TemplateWrapper.wrap(templateSource, ArrayUtils.add(TEMPLATES, 0, templatePath));

		Single<String> rs = renderService.rxRender(ctx, template).map(Buffer::toString);
		TestObserver<String> test = rs.test();

		rs.subscribe();

		test.await(3, TimeUnit.SECONDS);

		if (expectedError == null) {
			test.assertComplete()
				.assertResult(String.format("Template file %s: %s", fileId, templatePath));
		} else {
			test.assertError(expectedError);
		}
	}
}
