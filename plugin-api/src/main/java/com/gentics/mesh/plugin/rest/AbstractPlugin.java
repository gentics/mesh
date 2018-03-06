package com.gentics.mesh.plugin.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.gentics.mesh.plugin.Plugin;
import com.gentics.mesh.plugin.PluginManifest;

/**
 * Abstract implementation for a Gentics Mesh plugin.
 */
public abstract class AbstractPlugin implements Plugin, BundleActivator {

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

	@Override
	public abstract void start(BundleContext context) throws Exception;

	@Override
	public abstract void stop(BundleContext context) throws Exception;

	public List<Callable<RestExtension>> getExtensions() {
		return endpoints;
	}

	public void addExtension(Callable<RestExtension> extension) {
		endpoints.add(extension);
	}
}
