package com.gentics.mesh.plugin.forms;

import io.vertx.core.json.JsonObject;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.mesh.util.Tuple;

/**
 * Test cases for getting reporting data with special export options.
 */
@RunWith(Parameterized.class)
public class FormDataExportOptionsTest extends AbstractFormsPluginIntegrationTest {

	@Parameters(name = "{index}: include UUID {0}, include creation timestamp {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (boolean includeUuid : Arrays.asList(true, false)) {
			for (boolean includeCreationTimestamp : Arrays.asList(true, false)) {
				data.add(new Object[] { includeUuid, includeCreationTimestamp });
			}
		}

		return data;
	}

	@Parameter
	public boolean includeUuid;

	@Parameter(1)
	public boolean includeCreationTimestamp;

	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
		assertThat(setLanguages(PROJECT_NAME, Arrays.asList("en", "de"))).as("Activated languages").containsOnly("en", "de");

		FormsTestPlugin.instance.configuration.getExportOptions()
			.setIncludeUuid(includeUuid)
			.setIncludeCreationTimestamp(includeCreationTimestamp);
	}

	/**
	 * Test exporting data with default export options.
	 * @throws IOException
	 */
	@Test
	public void testExport() throws IOException {
		doExportTest();
	}

	/**
	 * Execute the export test
	 * @throws IOException
	 */
	protected void doExportTest() throws IOException {
		String uuid = createForm("threefields.json");

		// we collect the expected records (including the header) here as Set, because the actual order in
		// the CSV is indeterministic, so we will just check that all the records are there
		Set<String> records = new HashSet<>();
		// add "record" for the header
		List<String> headers = prepareHeaders("input1|||First Field", "input2|||Second Field", "input3|||Third Field");

		records.add(StringUtils.join(headers, ","));

		List<Tuple<String, String>> testList = Arrays.asList(
			Tuple.tuple("one", "one"),
			Tuple.tuple("new-line\ntwo", "new-line two"),
			Tuple.tuple("test,\"three\"", "\"test,\"\"three\"\"\"")
		);

		for (Tuple<String, String> value : testList) {
			RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", value.v1())
				.addFormDataPart("input2", value.v1())
				.addFormDataPart("input3", value.v1())
				.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/jsondata")).post(requestBody).build();
			Response response = client().newCall(postRequest).execute();
			// add the line to the set of expected records
			List<String> values = prepareValues(response, value.v2(), value.v2(), value.v2());

			records.add(StringUtils.join(values, ","));
		}

		Request exportRequest = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/export"))
			.headers(auth(true))
			.get()
			.build();
		Response exportResponse = client().newCall(exportRequest).execute();

		assertThat(exportResponse.isSuccessful()).as(String.format("Response success %d (%s)", exportResponse.code(), exportResponse.message())).isTrue();

		Set<String> actualRecords = new HashSet<>();
		try (BufferedReader in = new BufferedReader(new StringReader(exportResponse.body().string()))) {
			String line;

			while ((line = in.readLine()) != null) {
				actualRecords.add(line);
			}
		}

		assertThat(actualRecords).as("Exported data").containsOnlyElementsOf(records);
	}

	/**
	 * Prepare record from the export options and the given values.
	 *
	 * @param response The JSON response from the submit request
	 * @param values The expected record values
	 * @return A list containing all values for the record
	 */
	private List<String> prepareValues(Response response, String ...values) throws IOException {
		JsonObject responseData = null;

		if (includeUuid || includeCreationTimestamp) {
			responseData = new JsonObject(response.body().string());
		}

		List<String> record = new ArrayList<>();

		if (includeUuid) {
			record.add(responseData.getString(FIELD_RS_DATA_UUID));
		}

		if (includeCreationTimestamp) {
			record.add(responseData.getString(FIELD_RS_CREATED));
		}

		record.addAll(Arrays.asList(values));

		return record;
	}

	/**
	 * Prepare header fields from the export options and the given fields.
	 *
	 * @param fields The field names to add to the headers.
	 * @return A list containing all header fields
	 */
	private List<String> prepareHeaders(String ...fields) {
		List<String> headers = new ArrayList<>();

		if (includeUuid) {
			headers.add("uuid");
		}

		if (includeCreationTimestamp) {
			headers.add(FIELD_RS_CREATED);
		}

		headers.addAll(Arrays.asList(fields));

		return headers;
	}
}
