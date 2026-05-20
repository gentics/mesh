package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.model.CaptchaValidationException;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
public class FriendlyCaptchaProvider implements CaptchaProvider {
	private static final Logger log = LoggerFactory.getLogger(FriendlyCaptchaProvider.class);

	protected static final List<String> FAST_FAILER_SOLUTIONS = Arrays.asList(".FETCHING", ".UNSTARTED", ".UNFINISHED");

	private final FormsConfig configuration;
	private final OkHttpClientService okHttpClientService;

	@Inject
	public FriendlyCaptchaProvider(FormsConfig configuration, OkHttpClientService okHttpClientService) {
		this.configuration = configuration;
		this.okHttpClientService = okHttpClientService;
	}

	@Override
	public boolean supports(String provider) {
		return "friendlycaptcha".equals(provider);
	}

	@Override
	public Completable validate(FormDataCreateContext ctx) {
		String solutionKey = configuration.getFriendlyCaptcha().getSolutionKey();
		String solution = ctx.getContext().request().getHeader(solutionKey);
		return isValid(solution, okHttpClientService.getClient());
	}

	/**
	 * Check validity of the captcha response against the FriendlyCaptcha verification API.
	 * Completes normally if valid; errors with {@link CaptchaValidationException} if invalid,
	 * or {@link IllegalStateException} if the API returns a non-200 status.
	 */
	public Completable isValid(String captchaSolution, OkHttpClient httpClient) {
		log.debug("Trying to validate captchaResponse: {}", captchaSolution);

		if (StringUtils.isBlank(captchaSolution)) {
			return Completable.error(new CaptchaValidationException("Solution is empty"));
		} else if (FAST_FAILER_SOLUTIONS.contains(captchaSolution)) {
			return Completable.error(new CaptchaValidationException("Unacceptable solution"));
		} else {
			String jsonBody = new JsonObject()
					.put("response", captchaSolution)
					.put("sitekey", configuration.getFriendlyCaptcha().getSiteKey())
					.encode();

			RequestBody body = RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json"));

			Request request = new Request.Builder()
					.url(configuration.getFriendlyCaptcha().getVerificationUrl())
					.post(body)
					.header("X-API-Key", configuration.getFriendlyCaptcha().getSecretKey())
					.build();

			Call call = httpClient.newCall(request);
			return Single.<Response>create(sub -> {
				call.enqueue(new Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						if (!sub.isDisposed()) {
							log.error("OkHttp POST to FriendlyCaptcha failed.", e);
							sub.onSuccess(new Response.Builder().code(500).message(e.getMessage()).build());
						}
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException {
						if (!sub.isDisposed()) {
							sub.onSuccess(response);
						} else {
							response.close();
						}
					}
				});
			}).flatMapCompletable(response -> {
				int status = response.code();
				log.debug("Response status: {}", status);
				ResponseBody responseEntity = response.body();
				if (status == 200) {
					if (responseEntity == null) {
						throw new IllegalStateException("FriendlyCaptcha API returned status 200 with empty body");
					}
					String responseBody = responseEntity.string();
					JsonObject json = new JsonObject(responseBody);
					if (log.isDebugEnabled()) {
						log.debug("Response:\n{}", json.encodePrettily());
					}
					if (json.getBoolean("success", false)) {
						return Completable.complete();
					} else {
						JsonObject error = json.getJsonObject("error");
						String detail = (error != null)
								? error.getString("detail", "Unknown failure")
								: "Unknown failure";
						log.warn("FriendlyCaptcha validation failed, captchaId response: {}", detail);
						return Completable.error(new CaptchaValidationException(detail));
					}
				} else {
					String errorBody = responseEntity != null ? responseEntity.string() : "<empty>";
					if (status == 400 || status == 401) {
						log.error("FriendlyCaptcha configuration error (status={}): {}", status, errorBody);
						throw new IllegalStateException(String.format("FriendlyCaptcha configuration error (status=%d): %s", status, errorBody));
					}
					log.error("FriendlyCaptcha API returned unexpected status={}: {}", status, errorBody);
					throw new IllegalStateException(String.format("FriendlyCaptcha API returned unexpected status=%d: %s", status, errorBody));
				}
			});
		}
	}
}
