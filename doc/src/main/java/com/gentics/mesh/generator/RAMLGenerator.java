package com.gentics.mesh.generator;

import org.mockito.Mockito;
import org.raml.emitter.RamlEmitter;
import org.raml.model.Raml;

import com.gentics.mesh.core.verticle.node.NodeCrudHandler;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.etc.MeshSpringConfiguration;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;


public class RAMLGenerator {

	public static void main(String[] args) throws Exception {
		new RAMLGenerator().generator();
	}

	public void generator() throws Exception {
		Raml raml = new Raml();
		NodeVerticle verticle = Mockito.spy(new NodeVerticle());
		Mockito.when(verticle.getRouter()).thenReturn(Router.router(Vertx.vertx()));
		MeshSpringConfiguration mockConfig = Mockito.mock(MeshSpringConfiguration.class);
		NodeCrudHandler mockHandler = Mockito.mock(NodeCrudHandler.class);
		Mockito.when(verticle.getSpringConfiguration()).thenReturn(mockConfig);
		Mockito.when(verticle.getCrudHandler()).thenReturn(mockHandler);
		//verticle.init(null, null);
//		verticle.start();
		verticle.registerEndPoints();
		raml.getResources().put("/nodes", verticle.getResource());
		
//		NodeVerticle.addToRaml(raml);
//		Resource r = new Resource();
//		raml.getResources().put("adgds",r);
//		
//		r.setDescription("jow");
//		Resource r2 = new Resource();
//		r2.setDescription("egsed");
//		r2.setType("GET");
//		Action a = new Action();
//		a.setDisplayName("blar");
//		r2.getActions().put(ActionType.POST, a);
//		r.getResources().put("rre", r2);
//		raml.getResources().put("test", r);
//
//		raml.setTitle("test1234");
//
//		// modify the raml object

		RamlEmitter emitter = new RamlEmitter();
		String dumpFromRaml = emitter.dump(raml);
		System.out.println(dumpFromRaml);
	}
}
