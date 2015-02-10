package com.gentics.cailun.verticle.file;

import com.gentics.cailun.core.AbstractCailunRestVerticle;

public class FileVerticle extends AbstractCailunRestVerticle {

	protected FileVerticle(String basePath) {
		super("file");
	}

	@Override
	public void start() throws Exception {
		super.start();
		//http://docs.openstack.org/api/openstack-object-storage/1.0/content/ch_object-storage-dev-api-storage.html
		//http://docs.openstack.org/api/openstack-object-storage/1.0/content/storage_object_services.html
		// upload
		// download
		// delete
		// update
	}
}
