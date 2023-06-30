package com.gentics.mesh.plugin.forms;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.test.ResponseWithCookies;
import okhttp3.Cookie;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

public class FormPostVoteTest extends AbstractFormsPluginIntegrationTest {

	private static final String POLL_FORM = "simple-poll.json";
	private static final String POLL_FIELD = "checkbox1";

	@Before
	public void setup() {
		super.setup();

		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
	}

	/**
	 * Test if the node for the vote is created with the expected data, and that
	 * the data cookie is removed (set to empty string and expired).
	 */
	@Test
	public void testSubmitVote() throws IOException {
		String pollUuid = createForm(POLL_FORM);
		String vote = "option1";
		ResponseWithCookies rs = submit(pollUuid, true, true, POLL_FIELD, vote);
		Optional<Cookie> cookie = rs.getCookie(FormDataService.COOKIE_NAME_DATA);

		assertThat(cookie)
			.as("Data cookie")
			.isPresent();
		assertThat(cookie.get())
			.as("Data cookie")
			.matches(c -> "".equals(c.value()), "Value is empty")
			.matches(c -> c.expiresAt() < System.currentTimeMillis(), "is expired");

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, pollUuid));

		assertThat(dataNodes.getData().stream().filter(n -> !n.getSchema().getName().equals("form_" + pollUuid)))
			.as("Data node list")
			.hasSize(1);

		NodeResponse dataNode = dataNodes.getData().get(0);

		assertThat(dataNode.getFields().getStringField(POLL_FIELD).getString())
			.as("Data Node Field")
			.isEqualTo(vote);
		assertThat(dataNode.getVersion())
			.as("Data Node Version")
			.isEqualTo("1.0");
	}

	/**
	 * Test if the voted cookie is set and contains UUIDs of voted polls.
	 */
	@Test
	public void testVotedCookie() throws IOException {
		String vote = "option1";
		String firstPollUuid = createForm(POLL_FORM);
		ResponseWithCookies rs = submit(firstPollUuid, true, true, POLL_FIELD, vote);
		Optional<Cookie> cookie = rs.getCookie(FormDataService.COOKIE_NAME_VOTED);

		for (Cookie c : rs.cookies) {
			System.out.printf("Cookie %s: %s\n", c.name(), c.value());
		}

		assertThat(cookie)
			.as("Voted cookie (first submit)")
			.isPresent();
		assertThat(cookie.get().value())
			.as("Voted cookie (first submit)")
			.isEqualTo(firstPollUuid);

		// Create another poll using the same schema.
		String secondPollUuid = createForm(POLL_FORM);

		String votedCookie = String.format("%s=%s", FormDataService.COOKIE_NAME_VOTED, firstPollUuid);

		rs = submit(secondPollUuid, true, true, true, votedCookie, POLL_FIELD, vote);
		cookie = rs.getCookie(FormDataService.COOKIE_NAME_VOTED);

		for (Cookie c : rs.cookies) {
			System.out.printf("Cookie %s: %s\n", c.name(), c.value());
		}

		assertThat(cookie)
			.as("Voted cookie (second submit)")
			.isPresent();
		assertThat(cookie.get().value().split(Pattern.quote(FormDataService.COOKIE_VOTED_SEPARATOR)))
			.as("Voted cookie (second submit)")
			.containsExactlyInAnyOrder(firstPollUuid, secondPollUuid);

		// Submit data for the first poll again, and make sure the UUID in the cookie is not duplicated.
		votedCookie = String.format("%s=%s:%s", FormDataService.COOKIE_NAME_VOTED, firstPollUuid, secondPollUuid);
		rs = submit(firstPollUuid, true, true, true, votedCookie, POLL_FIELD, vote);
		cookie = rs.getCookie(FormDataService.COOKIE_NAME_VOTED);

		for (Cookie c : rs.cookies) {
			System.out.printf("Cookie %s: %s\n", c.name(), c.value());
		}

		assertThat(cookie)
			.as("Voted cookie (third submit)")
			.isPresent();
		assertThat(cookie.get().value().split(Pattern.quote(FormDataService.COOKIE_VOTED_SEPARATOR)))
			.as("Voted cookie (third submit)")
			.containsExactlyInAnyOrder(firstPollUuid, secondPollUuid);
	}

}
