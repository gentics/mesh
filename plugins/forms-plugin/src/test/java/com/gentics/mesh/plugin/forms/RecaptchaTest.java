package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import okhttp3.Cookie;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Test cases for reCAPTCHA service-level validation.
 */
public class RecaptchaTest extends AbstractFormPostDataTest {

	private static final String RECAPTCHA_SECRET_KEY = "your-secret-key";

	/**
	 * Test posting to a form with captcha configured but no captcha token sent.
	 */
	@Test
	public void testEmptyCaptcha() throws IOException {
		String uuid = createForm("recaptcha.json", true, true);

		// post only the input field — no x-recaptcha-token header
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("formdata", makeFormdataMultipart(uuid, null, Map.of("input1", "someValue")))
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody);

		// ReCaptchaProvider short-circuits before API call when token is missing
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(0);

		// we expect a 400 captcha error
		assertThat(pair.getLeft().code()).as("Response code").isEqualTo(400);
		JsonObject body = new JsonObject(pair.getLeft().body().string());
		assertThat(body.getString("error_code")).as("Error code").isEqualTo("CAPTCHA_ERROR");

		assertThat(getFormDataNodes(uuid)).as("Form Data Nodes").isEmpty();
		assertThat(pair.getRight()).as("Cookie").isNotPresent();
	}

	/**
	 * Test posting to a form with an incorrect captcha token.
	 */
	@Test
	public void testIncorrectCaptcha() throws IOException, InterruptedException {
		String uuid = createForm("recaptcha.json", true, true);

		// let the recaptcha verification server answer with success: false
		webServer.enqueue(new MockResponse().setBody("{\"success\": false}"));

		// post the input field; send the incorrect token as HTTP header
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("formdata", makeFormdataMultipart(uuid, null, Map.of("input1", "someValue")))
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody, Map.of("x-recaptcha-token", "incorrect"));

		// recaptcha verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body")
				.isEqualTo("[text=secret=%s&response=incorrect]".formatted(RECAPTCHA_SECRET_KEY));

		// we expect a 400 captcha error
		assertThat(pair.getLeft().code()).as("Response code").isEqualTo(400);
		JsonObject body = new JsonObject(pair.getLeft().body().string());
		assertThat(body.getString("error_code")).as("Error code").isEqualTo("CAPTCHA_ERROR");

		assertThat(getFormDataNodes(uuid)).as("Form Data Nodes").isEmpty();
		assertThat(pair.getRight()).as("Cookie").isNotPresent();
	}

	/**
	 * Test posting to a form with a correct captcha token.
	 */
	@Test
	public void testValidInput() throws IOException, InterruptedException {
		String uuid = createForm("recaptcha.json", true, true);

		// let the recaptcha verification server answer with success: true
		webServer.enqueue(new MockResponse().setBody("{\"success\": true}"));

		// post the input field; send the correct token as HTTP header
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("formdata", makeFormdataMultipart(uuid, null, Map.of("input1", "someValue")))
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody, Map.of("x-recaptcha-token", "correct"));

		// recaptcha verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body")
				.isEqualTo("[text=secret=%s&response=correct]".formatted(RECAPTCHA_SECRET_KEY));

		// answer to form post must have been OK
		assertOk(pair.getLeft());
	}

	/**
	 * Test posting to a multipage form with captcha configured but no captcha token sent.
	 */
	@Test
	public void testMultipageEmptyCaptcha() throws IOException {
		String uuid = createForm("multipage_recaptcha.json", true, true);

		// post input field from page 1 — no x-recaptcha-token header
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("formdata", makeFormdataMultipart(uuid, null, Map.of("input1", "someValue")))
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody);

		// ReCaptchaProvider short-circuits before API call when token is missing
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(0);

		// we expect a 400 captcha error
		assertThat(pair.getLeft().code()).as("Response code").isEqualTo(400);

		assertThat(getFormDataNodes(uuid)).as("Form Data Nodes").isEmpty();
		assertThat(pair.getRight()).as("Cookie").isNotPresent();
	}

	/**
	 * Test posting to a multipage form with an incorrect captcha token.
	 */
	@Test
	public void testMultipageIncorrectCaptcha() throws IOException, InterruptedException {
		String uuid = createForm("multipage_recaptcha.json", true, true);

		// let the recaptcha verification server answer with success: false
		webServer.enqueue(new MockResponse().setBody("{\"success\": false}"));

		// post the input field from the first page; send the incorrect token as HTTP header
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("page1", "1")
				.addFormDataPart("formdata", makeFormdataMultipart(uuid, null, Map.of("input1", "someValue")))
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody, Map.of("x-recaptcha-token", "incorrect"));

		// service makes one verification call per submission (not per element)
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body")
				.isEqualTo("[text=secret=%s&response=incorrect]".formatted(RECAPTCHA_SECRET_KEY));

		// we expect a 400 captcha error
		assertThat(pair.getLeft().code()).as("Response code").isEqualTo(400);
		JsonObject body = new JsonObject(pair.getLeft().body().string());
		assertThat(body.getString("error_code")).as("Error code").isEqualTo("CAPTCHA_ERROR");

		assertThat(getFormDataNodes(uuid)).as("Form Data Nodes").isEmpty();
		assertThat(pair.getRight()).as("Cookie").isNotPresent();
	}
}
