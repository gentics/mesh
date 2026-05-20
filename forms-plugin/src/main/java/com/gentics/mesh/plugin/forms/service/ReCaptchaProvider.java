package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.model.CaptchaValidationException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Singleton
public class ReCaptchaProvider implements CaptchaProvider {
	private static final Logger log = LoggerFactory.getLogger(ReCaptchaProvider.class);

	public static final String VERIFICATION_URL = "https://www.google.com/recaptcha/api/siteverify";

	private final FormsConfig configuration;
	private final OkHttpClientService okHttpClientService;

	@Inject
	public ReCaptchaProvider(FormsConfig configuration, OkHttpClientService okHttpClientService) {
		this.configuration = configuration;
		this.okHttpClientService = okHttpClientService;
	}

	@Override
	public boolean supports(String provider) {
		return "recaptcha".equals(provider);
	}

	@Override
	public Completable validate(FormDataCreateContext ctx) {
		String responseTokenKey = configuration.getReCaptcha().getResponseTokenKey();
		String responseToken = ctx.getContext().request().getHeader(responseTokenKey);
		return isValid(responseToken, okHttpClientService.getClient());
	}

	/**
	 * Check validity of the captcha response against the Google reCAPTCHA v2 verification API.
	 * Completes normally if valid; errors with {@link CaptchaValidationException} if invalid,
	 * or {@link IllegalStateException} for configuration or network errors.
	 */
	public Completable isValid(String responseToken, OkHttpClient httpClient) {
		log.debug("Trying to validate reCAPTCHA response: {}", responseToken);

		if (StringUtils.isBlank(responseToken)) {
			return Completable.error(new CaptchaValidationException("Response Token is empty"));
		}

		String secret = configuration.getReCaptcha().getSecretKey();
		if (StringUtils.isBlank(secret)) {
			return Completable.error(new IllegalStateException("reCAPTCHA secret key is not configured"));
		}

		String url = configuration.getReCaptcha().getVerificationUrl();

		FormBody formBody = new FormBody.Builder()
				.add("secret", secret)
				.add("response", responseToken)
				.build();

		Request request = new Request.Builder().url(url).post(formBody).build();

		Call call = httpClient.newCall(request);
		return Single.<Response>create(sub -> {
			sub.setCancellable(call::cancel);
			call.enqueue(new Callback() {
				@Override
				public void onFailure(Call call, IOException e) {
					if (!sub.isDisposed()) {
						log.error("OkHttp POST to reCAPTCHA API failed.", e);
						sub.onError(new IllegalStateException("reCAPTCHA verification request failed: " + e.getMessage(), e));
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
			log.debug("reCAPTCHA response status: {}", status);
			ResponseBody responseEntity = response.body();

			if (status >= 200 && status < 300) {
				if (responseEntity == null) {
					throw new IllegalStateException("reCAPTCHA API returned status 200 with empty body");
				}
				String responseBody = responseEntity.string();
				JsonObject json = new JsonObject(responseBody);
				if (log.isDebugEnabled()) {
					log.debug("reCAPTCHA response:\n{}", json.encodePrettily());
				}

				boolean success = json.getBoolean("success", false);
				if (success) {
					return Completable.complete();
				}

				List<String> errorCodes = JsonPath
						.parse(responseBody, Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build())
						.read("$.error-codes");

				if (errorCodes != null) {
					for (String code : errorCodes) {
						switch (code) {
						case "missing-input-secret":
						case "invalid-input-secret":
						case "bad-request":
							log.error("reCAPTCHA configuration error (error-code={}): {}", code, responseBody);
							throw new IllegalStateException("reCAPTCHA configuration error: " + code);
						default:
							break;
						}
					}
				}

				String detail = (errorCodes != null && !errorCodes.isEmpty()) ? String.join(", ", errorCodes) : "Unknown failure";
				log.warn("reCAPTCHA validation failed: {}", detail);
				return Completable.error(new CaptchaValidationException(detail));
			} else {
				String errorBody = responseEntity != null ? responseEntity.string() : "<empty>";
				log.error("reCAPTCHA API returned unexpected status={}: {}", status, errorBody);
				throw new IllegalStateException(String.format("reCAPTCHA API returned unexpected status=%d: %s", status, errorBody));
			}
		});
	}
}
