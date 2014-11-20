package com.gentics;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.data.Page;
import com.gentics.data.Tag;

public class MainVerticle extends AbstractVerticle {

	public static final Logger LOGGER = LoggerFactory
			.getLogger(MainVerticle.class);

	protected String dataVerticleDeploymentId;

	protected static Map<String, Tag> tags = new HashMap<String, Tag>();

	protected static Map<Long, Page> pages = new HashMap<Long, Page>();

	protected final static int NUM_PAGES = 100;

	static {
		generateTags();
		generatePages();
	}

	@Override
	public void start() {

		getVertx().deployVerticle(
				new DataVerticle(),
				new DeploymentOptions().setConfig(null).setInstances(10)
						.setMultiThreaded(false),
				new Handler<AsyncResult<String>>() {
					@Override
					public void handle(AsyncResult<String> event) {
						if (event.succeeded()) {
							dataVerticleDeploymentId = event.result();
							LOGGER.info("Deployed data workers");
						} else {
							LOGGER.error("Failed to deploy data workers",
									event.cause());
						}
					}
				});
		LOGGER.info("Started " + getClass().getName());
	}

	@Override
	public void stop() {
		if (dataVerticleDeploymentId != null) {
			getVertx().undeployVerticle(dataVerticleDeploymentId);
		}
		LOGGER.info("Stopped " + getClass().getName());
	}

	protected static void generateTags() {
		Tag year2013 = new Tag("2013");
		Tag year2014 = new Tag("2014");
		tags.put(year2013.getLabel(), year2013);
		tags.put(year2014.getLabel(), year2014);

		Calendar calendar = Calendar.getInstance();
		List<Tag> monthTags = new ArrayList<Tag>();
		for (int month = 0; month < 12; month++) {
			calendar.set(Calendar.MONTH, month);
			Tag monthTag = new Tag(calendar.getDisplayName(Calendar.MONTH,
					Calendar.LONG, Locale.ENGLISH));
			monthTags.add(monthTag);
			tags.put(monthTag.getLabel(), monthTag);
		}

		year2013.setChildren(monthTags);
		year2014.setChildren(monthTags);
	}

	protected static void generatePages() {
		Random rand = new Random();
		for (int i = 0; i < NUM_PAGES; i++) {
			Tag yearTag = tags.get(rand.nextBoolean() ? "2013" : "2014");
			Tag monthTag = yearTag.getChildren().get(rand.nextInt(12));

			Page page = new Page();
			page.setId((long) (i + 1));
			page.setTitle(randomText(15));
			page.setContent(randomText(200));
			page.setTags(Arrays.asList(yearTag, monthTag));

			pages.put(page.getId(), page);
		}
	}

	protected static String randomText(int length) {
		StringBuilder text = new StringBuilder(length);
		Random rand = new Random();
		for (int i = 0; i < length; i++) {
			int randInt = rand.nextInt(27);
			if (randInt == 26) {
				text.append(' ');
			} else {
				text.append((char) (randInt + 'a'));
			}
		}
		return text.toString();
	}
}
