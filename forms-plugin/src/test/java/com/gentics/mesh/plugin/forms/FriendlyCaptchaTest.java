package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import com.gentics.mesh.plugin.forms.model.FriendlyCaptchaElement;

import okhttp3.Cookie;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Test cases for the {@link FriendlyCaptchaElement}
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

		// post some data for the input field and a solution to the friendly captcha
		// (it does not matter, what we post here, because our fake verification server will accept anything)
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("input1", "some data")
				.addFormDataPart("frc-captcha-solution", solution)
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(formUuid, requestBody);
		Response response = pair.getLeft();

		// we expect the post to succeed
		assertThat(response.isSuccessful())
				.as("Response success %d (%s)".formatted(response.code(), response.message())).isTrue();

		// verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body").isEqualTo("[text=secret=%s&sitekey=%s&solution=%s]"
				.formatted(FRIENDLY_CAPTCHA_SECRET_KEY, FRIENDLY_CAPTCHA_SITE_KEY, solution));
	}

	/**
	 * Test that with empty captcha, no data is stored
	 * @throws IOException
	 */
	@Test
	public void testEmptyCaptcha() throws IOException {
		String formUuid = createForm("friendlycaptcha.json");

		// post some data for the input field, but nothing for the captcha
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("input1", "some data")
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(formUuid, requestBody);
		Response response = pair.getLeft();
		Optional<Cookie> cookie = pair.getRight();

		// we expect a redirect to the referer
		assertThat(response.isRedirect()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.header("Location")).as("Location Header").isEqualTo("ref");

		// we expect no data cookie to be set
		assertThat(cookie).as("Data cookie").isEmpty();

		// we expect not data being written
		assertThat(getFormDataNodes(formUuid)).as("Form Data Nodes").isEmpty();

		// we expect no calls to the recaptcha verification server
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(0);

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

		// post some data for the input field, but nothing for the captcha
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("input1", "some data")
				.addFormDataPart("frc-captcha-solution", solution)
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(formUuid, requestBody);
		Response response = pair.getLeft();
		Optional<Cookie> cookie = pair.getRight();

		// we expect a redirect to the referer
		assertThat(response.isRedirect()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.header("Location")).as("Location Header").isEqualTo("ref");

		// we expect no data cookie to be set
		assertThat(cookie).as("Data cookie").isEmpty();

		// we expect not data being written
		assertThat(getFormDataNodes(formUuid)).as("Form Data Nodes").isEmpty();

		// verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body").isEqualTo("[text=secret=%s&sitekey=%s&solution=%s]"
				.formatted(FRIENDLY_CAPTCHA_SECRET_KEY, FRIENDLY_CAPTCHA_SITE_KEY, solution));

	}
}
