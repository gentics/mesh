package com.gentics.mesh.graphdb.orientdb;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.mesh.etc.config.DiskQuotaOptions;
import com.gentics.mesh.graphdb.check.DiskQuotaChecker;
import com.gentics.mesh.mock.MockingLoggerRule;

import io.vertx.core.spi.logging.LogDelegate;

/**
 * Test cases for the disk quota checker
 */
public class DiskQuotaCheckerTest {
	private final static long MB = 1024 * 1024;

	@Rule
	public MockingLoggerRule rule = new MockingLoggerRule();

	protected LogDelegate logger = rule.get(DiskQuotaChecker.class.getName());

	/**
	 * Test that enough disk space usable will be accepted
	 * @throws Exception
	 */
	@Test
	public void testDiskOk() throws Exception {
		File storageDirectory = mock(File.class);
		when(storageDirectory.getTotalSpace()).thenReturn(100 * MB);
		when(storageDirectory.getUsableSpace()).thenReturn(80 * MB);
		DiskQuotaOptions options = new DiskQuotaOptions().setCheckInterval(1_000).setWarnThreshold("10M")
				.setReadOnlyThreshold("5M");
		@SuppressWarnings("unchecked")
		Consumer<Triple<Boolean, Long, Long>> resultConsumer = mock(Consumer.class);

		DiskQuotaChecker checker = new DiskQuotaChecker(storageDirectory, options, resultConsumer);
		checker.run();

		verify(resultConsumer).accept(Triple.of(false, 100 * MB, 80 * MB));
		verify(logger).info("Total space: 100 MB, usable: 80 MB (80%)");
		verify(logger, never()).warn(any());
		verify(logger, never()).error(any());
	}

	/**
	 * Test that for nearly not enough disk space, a warning will be logged
	 * @throws Exception
	 */
	@Test
	public void testDiskWarn() throws Exception {
		File storageDirectory = mock(File.class);
		when(storageDirectory.getTotalSpace()).thenReturn(100 * MB);
		when(storageDirectory.getUsableSpace()).thenReturn(8 * MB);
		DiskQuotaOptions options = new DiskQuotaOptions().setCheckInterval(1_000).setWarnThreshold("10M")
				.setReadOnlyThreshold("5M");
		@SuppressWarnings("unchecked")
		Consumer<Triple<Boolean, Long, Long>> resultConsumer = mock(Consumer.class);

		DiskQuotaChecker checker = new DiskQuotaChecker(storageDirectory, options, resultConsumer);
		checker.run();

		verify(resultConsumer).accept(Triple.of(false, 100 * MB, 8 * MB));
		verify(logger, never()).info(any());
		verify(logger).warn("Total space: 100 MB, usable: 8 MB (8%)");
		verify(logger, never()).error(any());
	}

	/**
	 * Test that not enough disk space usable will not be accepted
	 * @throws Exception
	 */
	@Test
	public void testDiskFull() throws Exception {
		File storageDirectory = mock(File.class);
		when(storageDirectory.getTotalSpace()).thenReturn(100 * MB);
		when(storageDirectory.getUsableSpace()).thenReturn(3 * MB);
		DiskQuotaOptions options = new DiskQuotaOptions().setCheckInterval(1_000).setWarnThreshold("10M")
				.setReadOnlyThreshold("5M");
		@SuppressWarnings("unchecked")
		Consumer<Triple<Boolean, Long, Long>> resultConsumer = mock(Consumer.class);

		DiskQuotaChecker checker = new DiskQuotaChecker(storageDirectory, options, resultConsumer);
		checker.run();

		verify(resultConsumer).accept(Triple.of(true, 100 * MB, 3 * MB));
		verify(logger, never()).info(any());
		verify(logger, never()).warn(any());
		verify(logger).error("Total space: 100 MB, usable: 3 MB (3%)");
	}
}
