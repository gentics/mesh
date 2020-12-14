package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.vertx.ext.web.RoutingContext;

/**
 * Include, Exclude options for the "include" query parameter of the debug info endpoint.
 */
public class IncludedInfo {

	private final Set<String> included;
	private final Set<String> excluded;

	public IncludedInfo(RoutingContext ac) {
		included = new HashSet<>();
		excluded = new HashSet<>();
		List<String> include = ac.queryParam("include");
		if (include.isEmpty()) {
			return;
		}
		Stream.of(include.get(0).split(","))
			.flatMap(part -> Inclusion.fromQueryPart(part).stream())
			.forEach(inclusion -> {
				if (inclusion.excluded) {
					excluded.add(inclusion.name);
				} else {
					included.add(inclusion.name);
				}
			});
	}

	public Set<String> getIncluded() {
		return included;
	}

	public Set<String> getExcluded() {
		return excluded;
	}

	private static class Inclusion {
		private final static Pattern pattern = Pattern.compile("([+-]?)(.*)");
		private final boolean excluded;
		private final String name;

		private Inclusion(boolean excluded, String name) {
			this.excluded = excluded;
			this.name = name;
		}

		public static Optional<Inclusion> fromQueryPart(String queryPart) {
			Matcher matcher = pattern.matcher(queryPart);
			if (!matcher.find()) {
				return Optional.empty();
			}
			boolean excluded = "-".equals(matcher.group(1));
			return Optional.ofNullable(matcher.group(2))
				.map(String::trim)
				.filter(name -> !name.isEmpty())
				.map(name -> new Inclusion(excluded, name));
		}

		public boolean isExcluded() {
			return excluded;
		}

		public String getName() {
			return name;
		}
	}
}
