package com.gentics.mesh.plugin.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.gentics.mesh.plugin.Plugin;
import com.gentics.mesh.plugin.PluginManifest;

/**
 * Abstract implementation for a Gentics Mesh plugin.
 */
public abstract class AbstractPlugin implements Plugin {

	private PluginManifest manifest;

	private List<Callable<RestExtension>> endpoints = new ArrayList<>();

	public AbstractPlugin() {
	}

	public PluginManifest getManifest() {
		return manifest;
	}

	public void setManifest(PluginManifest manifest) {
		this.manifest = manifest;
	}

	public abstract void start();

	public abstract void stop();

	public List<Callable<RestExtension>> getExtensions() {
		return endpoints;
	}

	public void addExtension(Callable<RestExtension> extension) {
		endpoints.add(extension);
	}
}
