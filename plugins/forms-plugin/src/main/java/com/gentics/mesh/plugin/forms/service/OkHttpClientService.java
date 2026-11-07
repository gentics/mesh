package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.config.ProxyOptions;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * A service for providing a HTTP client.
 */
public class OkHttpClientService {

	protected final Logger log = LoggerFactory.getLogger(OkHttpClientService.class);

	private final OkHttpClient client;

	public OkHttpClientService(FormsConfig config) {
		OkHttpClient theClient = new OkHttpClient();
		ProxyOptions proxyOptions = config.getProxyOptions();

		// set proxy options
		if (proxyOptions != null && proxyOptions.isActive()) {
			log.debug("Using proxy:\n{}", proxyOptions);
			int proxyPort = Integer.parseInt(proxyOptions.getPort());
			String proxyHost = proxyOptions.getHost();

			String proxyPassword = proxyOptions.getPassword();
			String proxyUsername = proxyOptions.getUser();

			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
			theClient = theClient.newBuilder().proxy(proxy).build();

			if (!StringUtils.isBlank(proxyUsername) && !StringUtils.isBlank(proxyPassword)) {
				Authenticator proxyAuthenticator = new Authenticator() {
					@Override
					public Request authenticate(Route route, Response response) throws IOException {
						String credential = Credentials.basic(proxyUsername, proxyPassword);
						return response.request().newBuilder().header("Proxy-Authorization", credential)
								.build();
					}
				};
				theClient = theClient.newBuilder().proxyAuthenticator(proxyAuthenticator).build();
			}
		}
		this.client = theClient;
	}

	/**
	 * Provide the client
	 * 
	 * @return
	 */
	public OkHttpClient getClient() {
		return client;
	}
}
