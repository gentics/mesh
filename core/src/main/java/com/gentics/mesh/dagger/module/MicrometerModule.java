package com.gentics.mesh.dagger.module;

import static com.gentics.mesh.util.StreamUtil.toStream;

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
import com.gentics.mesh.util.StreamUtil;

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

@Module
public class MicrometerModule {

	@Provides
	@Singleton
	public static MeterRegistry meterRegistry(MeshOptions options) {
		PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

		List<Tag> tags = Stream.of(
			createTag("nodeName", options.getNodeName()),
			createTag("clusterName", options.getClusterOptions().getClusterName())
		).flatMap(StreamUtil::toStream).collect(Collectors.toList());

		registry.config().commonTags(tags)
			.meterFilter(MeterFilter.replaceTagValues(Label.HTTP_PATH.toString(), MicrometerModule::replacePath));

		return registry;
	}

	private static final List<PathMatcher> regexMatcher = Arrays.asList(
		StaticRegexMatcher.projectMatch("webroot", "/webroot"),
		StaticRegexMatcher.projectMatch("graphql", "/graphql"),
		StaticRegexMatcher.projectMatch("nodes", "/nodes"),
		StaticRegexMatcher.projectMatch("binary", "/nodes/[^/]*/binary"),
		GroupRegexMatcher.apiPathMatch("plugin", "/plugins/([^/]*)"),
		GroupRegexMatcher.projectMatch("plugin", "/plugins/([^/]*)"),
		StaticRegexMatcher.projectMatch("search", "/search"),
		StaticRegexMatcher.projectMatch("search", "/rawSearch"),
		StaticRegexMatcher.apiPathMatch("search", "/search"),
		StaticRegexMatcher.apiPathMatch("search", "/rawSearch")
	);

	private static String replacePath(String actualPath) {
		return regexMatcher.stream()
			.flatMap(regex -> toStream(regex.getAlias(actualPath)))
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
				.setValue(options.getMonitoringOptions().getHost() + ":" + options.getMonitoringOptions().getPort())
		);
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
