package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.error.GenericRestException;

import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Test cases for data validation of a form of different input
 */
@RunWith(value = Parameterized.class)
public class FormDataValidationTest extends AbstractFormPostDataTest {

	@Parameters(name = "{index}: validator {0}, value {1}, succeeds {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		String validator = "date";
		// currently, there is no server-side validation for "datetime"
//		data.add(new Object[] { validator, "2026-04-10", true });
//		data.add(new Object[] { validator, "1970-01-01", true });
//		data.add(new Object[] { validator, "2126-04-23", true });
//		data.add(new Object[] { validator, "200000-04-23", false });
//		data.add(new Object[] { validator, "20000-04-23", false });
//		data.add(new Object[] { validator, "2000-004-23", false });
//		data.add(new Object[] { validator, "2000-04-023", false });
//		data.add(new Object[] { validator, "2000-4-23", false });
//		data.add(new Object[] { validator, "whatever", false });
//		data.add(new Object[] { validator, "   ", false });

		validator = "datepicker";
		// currently, there is no server-side validation for "datetime"
//		data.add(new Object[] { validator, "10.04.2026", true });
//		data.add(new Object[] { validator, "01.01.1970", true });
//		data.add(new Object[] { validator, "23.04.2126", true });
//		data.add(new Object[] { validator, "23.04.200000", false });
//		data.add(new Object[] { validator, "23.04.20000", false });
//		data.add(new Object[] { validator, "23.004.2000", false });
//		data.add(new Object[] { validator, "023.04.2000", false });
//		data.add(new Object[] { validator, "23.4.2000", false });
//		data.add(new Object[] { validator, "whatever", false });
//		data.add(new Object[] { validator, "   ", false });

		validator = "time";
		// currently, there is no server-side validation for "time"
//		data.add(new Object[] { validator, "12:33", true });
//		data.add(new Object[] { validator, "00:00", true });
//		data.add(new Object[] { validator, "0:0", false });
//		data.add(new Object[] { validator, "00", false });
//		data.add(new Object[] { validator, "000:00", false });
//		data.add(new Object[] { validator, "1234567", false });
//		data.add(new Object[] { validator, "whatever", false });
//		data.add(new Object[] { validator, "   ", false });

		validator = "number";
		// currently, there is no server-side validation for "number"
//		data.add(new Object[] { validator, "1234", true });
//		data.add(new Object[] { validator, "-5678", true });
//		data.add(new Object[] { validator, "12.34", false });
//		data.add(new Object[] { validator, "56,78", false });
//		data.add(new Object[] { validator, "1.234e-2", false });
//		data.add(new Object[] { validator, "whatever", false });
//		data.add(new Object[] { validator, "   ", false });

		validator = "telephone";
		data.add(new Object[] { validator, "+43000000", true });
		data.add(new Object[] { validator, "068134567", true });
		data.add(new Object[] { validator, "-123456", false });
		data.add(new Object[] { validator, "whatever", false });
		data.add(new Object[] { validator, "+43 681 9876543", true });
		data.add(new Object[] { validator, "0681 9876543", true });
		data.add(new Object[] { validator, "   ", false });

		validator = "socialsecurity";
		data.add(new Object[] { validator, "9146310870", true });
		data.add(new Object[] { validator, "9116070618", true });
		data.add(new Object[] { validator, "9842100308", true });
		data.add(new Object[] { validator, "whatever", false });
		data.add(new Object[] { validator, "1234567890", true });
		data.add(new Object[] { validator, "777777777777", false });
		data.add(new Object[] { validator, "   ", false });

		validator = "email";
		data.add(new Object[] { validator, "admin@gentics.com", true });
		data.add(new Object[] { validator, "a@gentics", false });
		data.add(new Object[] { validator, "a@b", false });
		data.add(new Object[] { validator, "whatever", false });
		data.add(new Object[] { validator, "@gentics.com", false });
		data.add(new Object[] { validator, "admin@", false });
		data.add(new Object[] { validator, "   ", false });

		return data;
	}

	@Parameter(0)
	public String validator;

	@Parameter(1)
	public String value;

	@Parameter(2)
	public boolean succeeds;

	@Before
	public void setup() {
		super.setup();
	}

	@After
	public void tearDown() throws IOException {
		super.tearDown();
	}

	@Test
	public void testValidation() throws GenericRestException, IOException {
		String uuid = createForm("validation/%s.json".formatted(validator), true, true);

		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("formdata",
						AbstractFormsPluginIntegrationTest.makeFormdataMultipart(uuid, null, Map.of("input1", value)))
				.build();
		okio.Buffer buffer = new okio.Buffer();
		requestBody.writeTo(buffer);

		Request postRequest = new Request.Builder()
				.url(projectUrl(String.format("/form/%s/validate?lang=de", uuid)))
				.header("Referer", "ref")
				.post(requestBody).build();

		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isEqualTo(true);

		JsonNode body = new ObjectMapper().readTree(response.body().string());

		assertThat(body.path("hasValidationErrors").isValueNode()).as("hasValidationErrors attribute is present").isTrue();
		assertThat(body.path("hasValidationErrors").asBoolean()).as("hasValidationErrors").isEqualTo(!succeeds);
	}
}
