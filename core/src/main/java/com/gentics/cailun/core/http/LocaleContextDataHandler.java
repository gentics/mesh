package com.gentics.cailun.core.http;

import io.vertx.core.Handler;
import io.vertx.ext.apex.RoutingContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.data.service.I18NService;

@Component
@Scope("singleton")
public class LocaleContextDataHandler implements Handler<RoutingContext> {

	@Autowired
	I18NService i18n;

	@Override
	public void handle(RoutingContext rc) {
		rc.put("locale", i18n.getLocale(rc));
		rc.next();
	}

}
