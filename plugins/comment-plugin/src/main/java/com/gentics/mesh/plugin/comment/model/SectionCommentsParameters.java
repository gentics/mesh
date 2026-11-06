package com.gentics.mesh.plugin.comment.model;


import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

public class SectionCommentsParameters {
	private static final Order DEFAULT_ORDER = Order.ASCENDING;
	private static final String DEFAULT_LANGUAGE = "en";

	private String language = DEFAULT_LANGUAGE;
	private Order order = DEFAULT_ORDER;

	public static SectionCommentsParameters fromContext(RoutingContext context) {
		return fromContext(context.queryParams());
	}

	public static SectionCommentsParameters fromContext(MultiMap params) {
		SectionCommentsParameters commentParams = new SectionCommentsParameters();
		Optional.ofNullable(params.get("language")).ifPresent(commentParams::setLanguage);
		Optional.ofNullable(params.get("order")).map(Order::fromString).ifPresent(commentParams::setOrder);
		return commentParams;
	}

	private SectionCommentsParameters() {
	}

	public String getLanguage() {
		return language;
	}

	public SectionCommentsParameters setLanguage(String language) {
		this.language = language;
		return this;
	}

	public Order getOrder() {
		return order;
	}

	public SectionCommentsParameters setOrder(Order order) {
		this.order = order;
		return this;
	}
}
