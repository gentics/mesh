package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.example.ExampleUuids.MICROSCHEMA_UUID;

import com.gentics.mesh.core.rest.branch.info.BranchInfoMicroschemaList;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;

public class MicroschemaExamples extends AbstractExamples {

	public BranchInfoMicroschemaList createMicroschemaReferenceList() {
		BranchInfoMicroschemaList microschemas = new BranchInfoMicroschemaList();
		microschemas.add(getMicroschemaReference("vcard", "2.0"));
		microschemas.add(getMicroschemaReference("geolocation", "1.0"));
		return microschemas;
	}

	public MicroschemaResponse getGeolocationMicroschemaResponse() {
		MicroschemaResponse microschema = new MicroschemaResponse();
		microschema.setName("geolocation");
		microschema.setDescription("Microschema for Geolocations");
		microschema.setVersion("1.0");
		microschema.setUuid(MICROSCHEMA_UUID);

		NumberFieldSchema longitudeFieldSchema = new NumberFieldSchemaImpl();
		longitudeFieldSchema.setName("longitude");
		longitudeFieldSchema.setLabel("Longitude");
		longitudeFieldSchema.setRequired(true);
		//		longitudeFieldSchema.setMin(-180);
		//		longitudeFieldSchema.setMax(180);
		microschema.addField(longitudeFieldSchema);

		NumberFieldSchema latitudeFieldSchema = new NumberFieldSchemaImpl();
		latitudeFieldSchema.setName("latitude");
		latitudeFieldSchema.setLabel("Latitude");
		latitudeFieldSchema.setRequired(true);
		//		latitudeFieldSchema.setMin(-90);
		//		latitudeFieldSchema.setMax(90);
		microschema.addField(latitudeFieldSchema);

		microschema.setPermissions(READ, UPDATE, DELETE, CREATE);
		microschema.validate();
		return microschema;
	}

	public MicroschemaListResponse getMicroschemaListResponse() {
		MicroschemaListResponse microschemaList = new MicroschemaListResponse();
		microschemaList.getData().add(getGeolocationMicroschemaResponse());
		microschemaList.getData().add(getGeolocationMicroschemaResponse());
		setPaging(microschemaList, 1, 10, 2, 20);
		return microschemaList;
	}

	public MicroschemaCreateRequest getGeolocationMicroschemaCreateRequest() {
		MicroschemaCreateRequest createRequest = new MicroschemaCreateRequest();
		createRequest.setName("geolocation");
		createRequest.setDescription("Microschema for Geolocations");
		NumberFieldSchema longitudeFieldSchema = new NumberFieldSchemaImpl();
		longitudeFieldSchema.setName("longitude");
		longitudeFieldSchema.setLabel("Longitude");
		longitudeFieldSchema.setRequired(true);
		//		longitudeFieldSchema.setMin(-180);
		//		longitudeFieldSchema.setMax(180);
		createRequest.addField(longitudeFieldSchema);

		NumberFieldSchema latitudeFieldSchema = new NumberFieldSchemaImpl();
		latitudeFieldSchema.setName("latitude");
		latitudeFieldSchema.setLabel("Latitude");
		latitudeFieldSchema.setRequired(true);
		//		latitudeFieldSchema.setMin(-90);
		//		latitudeFieldSchema.setMax(90);
		createRequest.addField(latitudeFieldSchema);

		return createRequest;
	}

	public MicroschemaUpdateRequest getGeolocationMicroschemaUpdateRequest() {
		MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
		request.setName("geolocation-renamed");
		return request;
	}
}
