package com.gentics.mesh.cache;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.node.AbstractMassiveNodeLoadTest;

import ch.qos.logback.core.util.Duration;

public abstract class AbstractMassivePublishTest extends AbstractMassiveNodeLoadTest {

	public AbstractMassivePublishTest() {
		super(10000);
	}

	@Before
	public void makeEmAll() {
		// No make yet
	}

	@Test
	public void testAndMeasureMassivePublish() {
		Instant started = Instant.now();
		System.out.println(String.format("Publish started at %s", DateTimeFormatter.ISO_INSTANT.format(started)));
		makeEmAll(numOfNodesPerLevel, tx(() -> project().getBaseNode().getUuid()), true);
		Instant ended = Instant.now();
		System.out.println(String.format("Publish ended at %s, duration %s", DateTimeFormatter.ISO_INSTANT.format(ended), Duration.buildByMilliseconds(ended.toEpochMilli() - started.toEpochMilli())));
	}
}
