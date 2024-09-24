package com.gentics.mesh.hibernate.data.dao;

import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;

import dagger.Lazy;
import io.vertx.core.Vertx;

public abstract class AbstractImageDataHibDao<T extends HibImageDataElement> extends AbstractHibDao<T> {

	

	public AbstractImageDataHibDao(HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper,
			CurrentTransaction currentTransaction, EventFactory eventFactory, Lazy<Vertx> vertx) {
		super(permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
	}

	@Override
	public String mapGraphQlFilterFieldName(String gqlName) {
		switch (gqlName) {
		case "filename": return "fileName";
		case "imageDominantColor": return "imageDominantColor";
		case "mime": return "mimeType";
		case "plainText": return "plainText";
		case "altitude": return "locationAltitude";
		case "latitude": return "locationLatitude";
		case "longitude": return "locationLongitude";
		case "focalPointX": return "focalPointX";
		case "focalPointY": return "focalPointY";
		}
		return super.mapGraphQlFilterFieldName(gqlName);
	}
}
