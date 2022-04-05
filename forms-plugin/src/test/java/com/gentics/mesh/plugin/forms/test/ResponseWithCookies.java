package com.gentics.mesh.plugin.forms.test;

import com.gentics.mesh.plugin.forms.service.FormDataService;
import okhttp3.Cookie;
import okhttp3.Response;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class ResponseWithCookies {

	public final Response response;
	public final Collection<Cookie> cookies;

	public ResponseWithCookies(Response response, Collection<Cookie> cookies) {
		this.response = response;
		this.cookies = Collections.unmodifiableCollection(cookies);
	}

	public Optional<Cookie> getCookie(String name) {
		return StringUtils.isBlank(name)
			? Optional.empty()
			: cookies.stream().filter(c -> name.equals(c.name())).findFirst();
	}
}
