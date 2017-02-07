package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;

import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReferenceList;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.util.UUIDUtil;

public class MicroschemaExamples extends AbstractExamples {

	public MicroschemaReferenceList createMicroschemaReferenceList() {
		MicroschemaReferenceList microschemas = new MicroschemaReferenceList();
		microschemas.add(getMicroschemaReference("vcard", 2));
		microschemas.add(getMicroschemaReference("geolocation", 1));
		return microschemas;
	}

	public Microschema getGeolocationMicroschema() {
		Microschema microschema = new MicroschemaModel();
		microschema.setName("geolocation");
		microschema.setDescription("Microschema for Geolocations");
		microschema.setVersion(1);
		microschema.setUuid(UUIDUtil.randomUUID());

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
		microschemaList.getData().add(getGeolocationMicroschema());
		microschemaList.getData().add(getGeolocationMicroschema());
		setPaging(microschemaList, 1, 10, 2, 20);
		return microschemaList;
	}

	public Microschema getGeolocationMicroschemaCreateRequest() {
		Microschema createRequest = new MicroschemaModel();
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
}
