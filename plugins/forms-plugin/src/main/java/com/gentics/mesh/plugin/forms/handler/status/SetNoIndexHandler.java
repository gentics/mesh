package com.gentics.mesh.plugin.forms.handler.status;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.service.StatusService;

import io.reactivex.Completable;

public class SetNoIndexHandler extends AbstractNoIndexHandler {

	@Inject
	public SetNoIndexHandler(StatusService statusService, ObjectMapper mapper) {
		super(statusService, mapper);
	}

	@Override
	public Completable handle(PluginContext context) {
		return handle(context, true);
	}
}