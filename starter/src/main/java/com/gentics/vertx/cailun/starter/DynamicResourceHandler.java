package com.gentics.vertx.cailun.starter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;


public class DynamicResourceHandler {

	public void evaluate() throws ScriptException {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("nashorn");

		String js;

//		js = "var map = Array.prototype.map \n";
//		js += "var names = [\"john\", \"jerry\", \"bob\"]\n";
//		js += "var a = map.call(names, function(name) { return name.length() })\n";
//		js += "print(a)"
	js = "";

		Object result = engine.eval(js);
	}

	public void addDynamicResource(ResourceConfig config) {
		final Resource.Builder resourceBuilder = Resource.builder();
		resourceBuilder.path("helloworld");

		final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");
		methodBuilder.produces(MediaType.TEXT_PLAIN_TYPE).handledBy(new Inflector<ContainerRequestContext, String>() {

			@Override
			public String apply(ContainerRequestContext containerRequestContext) {
				return "Hello World!";
			}
		});

		final Resource resource = resourceBuilder.build();
		config.registerResources(resource);
	}
}
