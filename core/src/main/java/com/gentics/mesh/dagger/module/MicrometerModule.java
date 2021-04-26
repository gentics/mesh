package com.gentics.mesh.dagger.module;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;

import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.Match;
import io.vertx.micrometer.MetricsDomain;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

/**
 * Dagger module for micrometer related options and registries.
 */
@Module
public class MicrometerModule {

	/**
	 * Create the meter registry for the given mesh options. The registry will automatically be tagged to include information about the nodeName and cluster
	 * name.
	 * 
	 * @param options
	 * @return
	 */
	@Provides
	@Singleton
	public static MeterRegistry meterRegistry(MeshOptions options) {
		PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

		List<Tag> tags = Stream.of(
			createTag("nodeName", options.getNodeName()),
			createTag("clusterName", options.getClusterOptions().getClusterName())).flatMap(Optional::stream).collect(Collectors.toList());

		registry.config().commonTags(tags)
			.meterFilter(MeterFilter.replaceTagValues(Label.HTTP_PATH.toString(), MicrometerModule::replacePath));

		return registry;
	}

	private static final List<PathMatcher> regexMatcher = Arrays.asList(
		StaticRegexMatcher.projectMatch("webrootfield", "/webrootfield/([_a-zA-Z][_a-zA-Z0-9]*)/(.*)"),
		StaticRegexMatcher.projectMatch("webroot", "/webroot"),
		StaticRegexMatcher.projectMatch("graphql", "/graphql"),
		StaticRegexMatcher.projectMatch("search", "/search"),
		StaticRegexMatcher.projectMatch("search", "/rawSearch"),
		StaticRegexMatcher.apiPathMatch("search", "/search"),
		StaticRegexMatcher.apiPathMatch("search", "/rawSearch"),
		StaticRegexMatcher.projectMatch("binary", "/nodes/[^/]*/binary"),
		StaticRegexMatcher.projectMatch("nodes", "/nodes"),
		GroupRegexMatcher.apiPathMatch("plugin", "/plugins/([^/]*)"),
		GroupRegexMatcher.projectMatch("plugin", "/plugins/([^/]*)"));

	private static String replacePath(String actualPath) {
		return regexMatcher.stream()
			.flatMap(regex -> regex.getAlias(actualPath).stream())
			.findAny()
			.orElse("other");
	}

	private static Optional<Tag> createTag(String key, String value) {
		if (key == null || value == null) {
			return Optional.empty();
		} else {
			return Optional.of(Tag.of(key, value));
		}
	}

	/**
	 * Generate the micrometer options using the provided mesh options.
	 * 
	 * @param options
	 *            Mesh options
	 * @param meterRegistry
	 *            Reference to the to be used meter registry
	 * @return
	 */
	@Provides
	@Singleton
	public static MetricsOptions micrometerMetricsOptions(MeshOptions options, MeterRegistry meterRegistry) {
		MonitoringConfig monitoringOptions = options.getMonitoringOptions();
		MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
			.setMicrometerRegistry(meterRegistry)
			.setRegistryName(options.getNodeName())
			.setJvmMetricsEnabled(monitoringOptions.isJvmMetricsEnabled())
			.setLabels(EnumSet.of(Label.HTTP_CODE, Label.HTTP_METHOD, Label.LOCAL, Label.HTTP_PATH))
			.setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
			.setEnabled(true);
		labelMatches(options).forEach(metricsOptions::addLabelMatch);

		return metricsOptions;
	}

	private static Stream<Match> labelMatches(MeshOptions options) {
		return Stream.of(
			new Match()
				.setDomain(MetricsDomain.HTTP_SERVER)
				.setLabel("local")
				.setAlias("restapi")
				.setValue(options.getHttpServerOptions().getHost() + ":" + options.getHttpServerOptions().getPort()),
			new Match()
				.setDomain(MetricsDomain.HTTP_SERVER)
				.setLabel("local")
				.setAlias("monitoring")
				.setValue(options.getMonitoringOptions().getHost() + ":" + options.getMonitoringOptions().getPort()));
	}

	interface PathMatcher {
		Optional<String> getAlias(String path);
	}

	private static class StaticRegexMatcher implements PathMatcher {
		private final String alias;
		private final Pattern pattern;

		private StaticRegexMatcher(String alias, Pattern pattern) {
			this.alias = alias;
			this.pattern = pattern;
		}

		private static StaticRegexMatcher projectMatch(String alias, String path) {
			return apiPathMatch(alias, "/[^/]*" + path);
		}

		private static StaticRegexMatcher apiPathMatch(String alias, String regex) {
			return new StaticRegexMatcher(alias, Pattern.compile("/api/v\\d+" + regex));
		}

		@Override
		public Optional<String> getAlias(String path) {
			if (pattern.matcher(path).find()) {
				return Optional.of(alias);
			} else {
				return Optional.empty();
			}
		}
	}

	private static class GroupRegexMatcher implements PathMatcher {
		private final String aliasPrefix;
		private final Pattern pattern;

		private GroupRegexMatcher(String aliasPrefix, Pattern pattern) {
			this.aliasPrefix = aliasPrefix;
			this.pattern = pattern;
		}

		public static GroupRegexMatcher projectMatch(String aliasPrefix, String path) {
			return apiPathMatch(aliasPrefix, "/[^/]*" + path);
		}

		public static GroupRegexMatcher apiPathMatch(String aliasPrefix, String path) {
			return new GroupRegexMatcher(aliasPrefix, Pattern.compile("/api/v\\d+" + path));
		}

		@Override
		public Optional<String> getAlias(String path) {
			Matcher matcher = pattern.matcher(path);
			if (matcher.find()) {
				return Optional.of(IntStream.rangeClosed(1, matcher.groupCount())
					.mapToObj(matcher::group)
					.collect(Collectors.joining("_", aliasPrefix + "_", "")));
			} else {
				return Optional.empty();
			}
		}
	}
}
