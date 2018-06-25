package com.gentics.mesh.jmh.model;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

public class JMHResult implements RestModel {

	private String version;

	private String jmhVersion = "1.21";

	private String benchmark;

	private String mode = "avgt";

	private int threads = 1;

	private int forks = 1;

	private String jvm;

	private List<String> jvmArgs = Collections.emptyList();

	private String jdkVersion = ManagementFactory.getRuntimeMXBean().getVmVersion();

	private String vmName = ManagementFactory.getRuntimeMXBean().getVmName();

	private String vmVersion = ManagementFactory.getRuntimeMXBean().getVmVersion();

	private int warmupIterations = 2;

	private String warmupTime = "1 s";

	private int warmupBatchSize = 1;

	private int measurementIterations = 2;

	private String measurementTime = "1 s";

	private int measurementBatchSize = 1;

	private JMHMetric primaryMetric = new JMHMetric();

	private JMHMetric secondaryMetrics = new JMHMetric();

	public String getJmhVersion() {
		return jmhVersion;
	}

	public JMHResult setJmhVersion(String jmhVersion) {
		this.jmhVersion = jmhVersion;
		return this;
	}

	public String getBenchmark() {
		return benchmark;
	}

	public JMHResult setBenchmark(String benchmark) {
		this.benchmark = benchmark;
		return this;
	}

	public String getMode() {
		return mode;
	}

	public JMHResult setMode(String mode) {
		this.mode = mode;
		return this;
	}

	public int getThreads() {
		return threads;
	}

	public JMHResult setThreads(int threads) {
		this.threads = threads;
		return this;
	}

	public int getForks() {
		return forks;
	}

	public JMHResult setForks(int forks) {
		this.forks = forks;
		return this;
	}

	public String getJvm() {
		return jvm;
	}

	public JMHResult setJvm(String jvm) {
		this.jvm = jvm;
		return this;
	}

	public List<String> getJvmArgs() {
		return jvmArgs;
	}

	public JMHResult setJvmArgs(List<String> jvmArgs) {
		this.jvmArgs = jvmArgs;
		return this;
	}

	public String getJdkVersion() {
		return jdkVersion;
	}

	public JMHResult setJdkVersion(String jdkVersion) {
		this.jdkVersion = jdkVersion;
		return this;
	}

	public String getVmName() {
		return vmName;
	}

	public JMHResult setVmName(String vmName) {
		this.vmName = vmName;
		return this;
	}

	public String getVmVersion() {
		return vmVersion;
	}

	public JMHResult setVmVersion(String vmVersion) {
		this.vmVersion = vmVersion;
		return this;
	}

	public int getWarmupIterations() {
		return warmupIterations;
	}

	public JMHResult setWarmupIterations(int warmupIterations) {
		this.warmupIterations = warmupIterations;
		return this;
	}

	public String getWarmupTime() {
		return warmupTime;
	}

	public JMHResult setWarmupTime(String warmupTime) {
		this.warmupTime = warmupTime;
		return this;
	}

	public int getWarmupBatchSize() {
		return warmupBatchSize;
	}

	public JMHResult setWarmupBatchSize(int warmupBatchSize) {
		this.warmupBatchSize = warmupBatchSize;
		return this;
	}

	public int getMeasurementIterations() {
		return measurementIterations;
	}

	public JMHResult setMeasurementIterations(int measurementIterations) {
		this.measurementIterations = measurementIterations;
		return this;
	}

	public String getMeasurementTime() {
		return measurementTime;
	}

	public JMHResult setMeasurementTime(String measurementTime) {
		this.measurementTime = measurementTime;
		return this;
	}

	public int getMeasurementBatchSize() {
		return measurementBatchSize;
	}

	public JMHResult setMeasurementBatchSize(int measurementBatchSize) {
		this.measurementBatchSize = measurementBatchSize;
		return this;
	}

	public JMHMetric getPrimaryMetric() {
		return primaryMetric;
	}

	public JMHResult setPrimaryMetric(JMHMetric primaryMetric) {
		this.primaryMetric = primaryMetric;
		return this;
	}

	public JMHMetric getSecondaryMetrics() {
		return secondaryMetrics;
	}

	public JMHResult setSecondaryMetrics(JMHMetric secondaryMetrics) {
		this.secondaryMetrics = secondaryMetrics;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public JMHResult setVersion(String version) {
		this.version = version;
		return this;
	}

}
