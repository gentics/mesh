package com.gentics.mesh.plugin.comment;

import io.vertx.reactivex.ext.web.RoutingContext;
import org.mockito.AdditionalAnswers;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestRoutingContext {
	private final Map<String, Object> data = new HashMap<>();

	public <T> T get(Object key) {
		return (T) data.get(key);
	}

	public TestRoutingContext put(String key, Object value) {
		data.put(key, value);
		return this;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public RoutingContext asContext() {
		RoutingContext context = mock(RoutingContext.class);
		when(context.get(anyString())).then(AdditionalAnswers.delegatesTo(data));
		when(context.put(anyString(), any())).then(inv -> {
			String key = inv.getArgument(0);
			Object value = inv.getArgument(1);
			put(key, value);
			return inv.getMock();
		});
		return context;
	}
}
