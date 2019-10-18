package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.ext.web.RoutingContext;

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
			.map(Inclusion::new)
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

		public Inclusion(String queryPart) {
			Matcher matcher = pattern.matcher(queryPart);
			this.excluded = "-".equals(matcher.group(1));
			this.name = matcher.group(2);
		}

		public boolean isExcluded() {
			return excluded;
		}

		public String getName() {
			return name;
		}
	}
}
