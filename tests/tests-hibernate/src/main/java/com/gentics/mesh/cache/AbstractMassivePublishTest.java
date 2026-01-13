package com.gentics.mesh.cache;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;

import com.gentics.mesh.core.node.AbstractMassiveNodeLoadTest;
import com.gentics.mesh.util.UUIDUtil;

import ch.qos.logback.core.util.Duration;

public abstract class AbstractMassivePublishTest extends AbstractMassiveNodeLoadTest {

	/**
	 * Global Test Timeout of 60 Minutes, to fit 'em all
	 */
	public static Timeout globalTimeout= new Timeout(60, TimeUnit.MINUTES);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(globalTimeout).around(testContext);

	protected static final UUID VERY_BASE_UUID = UUIDUtil.toJavaUuid("6B06697A9ABE11F096B6F7559C093C6E");

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
		makeEmAll(numOfNodesPerLevel, tx(() -> project().getBaseNode().getUuid()), true, getMaybeUuidProvider());
		Instant ended = Instant.now();
		System.out.println(String.format("Publish ended at %s, duration %s", DateTimeFormatter.ISO_INSTANT.format(ended), Duration.buildByMilliseconds(ended.toEpochMilli() - started.toEpochMilli())));
	}

	protected abstract Optional<BiFunction<UUID, Integer, UUID>> getMaybeUuidProvider();
}
