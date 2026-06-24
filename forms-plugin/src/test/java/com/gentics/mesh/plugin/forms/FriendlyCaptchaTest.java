package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import okhttp3.Cookie;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Test cases for FriendlyCaptcha service-level validation.
 */
public class FriendlyCaptchaTest extends AbstractFormPostDataTest {
	/**
	 * Test that with valid captcha, the data is stored
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testValidInput() throws IOException, InterruptedException {
		String solution = "correct";
		String formUuid = createForm("friendlycaptcha.json");

		// let the captcha verification server answer with success: true
		webServer.enqueue(new MockResponse().setBody("{\"success\": true}"));

		// post form data and send the captcha solution as HTTP header
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("formdata", makeFormdataMultipart(formUuid, null, Map.of("input1", "some data")))
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(formUuid, requestBody, Map.of("frc-captcha-response", solution));
		Response response = pair.getLeft();

		// we expect the post to succeed
		assertThat(response.isSuccessful())
				.as("Response success %d (%s)".formatted(response.code(), response.message())).isTrue();

		// verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to friendlycaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body").isEqualTo(
				"[text={\"response\":\"%s\",\"sitekey\":\"%s\"}]".formatted(solution, FRIENDLY_CAPTCHA_SITE_KEY));
	}

	/**
	 * Test that with empty captcha, no data is stored
	 * @throws IOException
	 */
	@Test
	public void testEmptyCaptcha() throws IOException {
		String formUuid = createForm("friendlycaptcha.json");

		// post form data without a captcha header — FriendlyCaptchaProvider short-circuits before calling the API
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("formdata", makeFormdataMultipart(formUuid, null, Map.of("input1", "some data")))
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(formUuid, requestBody);
		Optional<Cookie> cookie = pair.getRight();

		// we expect no data cookie to be set
		assertThat(cookie).as("Data cookie").isEmpty();

		// we expect no data being written
		assertThat(getFormDataNodes(formUuid)).as("Form Data Nodes").isEmpty();

		// we expect no calls to the friendlycaptcha verification server
		assertThat(webServer.getRequestCount()).as("Requests to friendlycaptcha").isEqualTo(0);
	}

	/**
	 * Test that with an incorrect captcha, no data is stored
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	@Test
	public void testIncorrectCaptcha() throws IOException, InterruptedException {
		String solution = "incorrect";
		String formUuid = createForm("friendlycaptcha.json");

		// let the captcha verification server answer with success: false
		webServer.enqueue(new MockResponse().setBody("{\"success\": false}"));

		// post form data and send the captcha solution as HTTP header
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("formdata", makeFormdataMultipart(formUuid, null, Map.of("input1", "some data")))
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(formUuid, requestBody, Map.of("frc-captcha-response", solution));
		Optional<Cookie> cookie = pair.getRight();

		// we expect no data cookie to be set
		assertThat(cookie).as("Data cookie").isEmpty();

		// we expect no data being written
		assertThat(getFormDataNodes(formUuid)).as("Form Data Nodes").isEmpty();

		// verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to friendlycaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body").isEqualTo(
				"[text={\"response\":\"%s\",\"sitekey\":\"%s\"}]".formatted(solution, FRIENDLY_CAPTCHA_SITE_KEY));
	}
}
