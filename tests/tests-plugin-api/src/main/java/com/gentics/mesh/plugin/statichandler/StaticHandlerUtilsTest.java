package com.gentics.mesh.plugin.statichandler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.gentics.mesh.plugin.RestPlugin;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

public class StaticHandlerUtilsTest {

    private RestPlugin plugin;
    private Vertx vertx;
    private File storageDir;

    @Before
    public void setup() throws IOException {
        vertx = Vertx.vertx();
        storageDir = Files.createTempDirectory("mesh-static-handler-test-").toFile();

        plugin = mock(RestPlugin.class, CALLS_REAL_METHODS); // execute the default methods
        when(plugin.vertx()).thenReturn(vertx);
        when(plugin.getStorageDir()).thenReturn(storageDir);
    }

    @Test
    public void testFolderRequestReturns404() throws Exception {
        // Mock the route and capture the handler configured by addStaticHandlerFromClasspath
        Route route = mock(Route.class);
        ArgumentCaptor<Handler<RoutingContext>> handlerCaptor = ArgumentCaptor.forClass(Handler.class); // captures the prehandler
        when(route.handler(handlerCaptor.capture())).thenReturn(route);

        // Use a base which is guaranteed to exist as a directory in the test classpath.
        // Use the package of this test class as a deterministic directory base.
        plugin.addStaticHandlerFromClasspath(route, "com/gentics/mesh/plugin/statichandler", null);

        Handler<RoutingContext> handler = handlerCaptor.getAllValues().get(0); //get the prehandler

        RoutingContext rc = mock(RoutingContext.class);
        when(rc.normalisedPath()).thenReturn("/");

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(rc).fail(404);

        Enumeration<URL> roots =
                getClass().getClassLoader().getResources("com/gentics/mesh/plugin/statichandler/");

        System.out.println("=== Classpath roots for com/gentics/mesh/plugin/statichandler/ ===");
        while (roots.hasMoreElements()) {
            System.out.println(roots.nextElement());
        }

        System.out.println(getClass().getResource("StaticHandlerUtilsTest.class"));
        System.out.println("================================");

        handler.handle(rc);
        // The directory detection branch uses async file system calls, so wait briefly.
        assertTrue("Expected rc.fail(404) to be called", latch.await(5, TimeUnit.SECONDS)); // wait 2 seconds until latch is decresed to 0
        verify(rc).fail(404);
    }

    @Test
    public void testFileRequestCallsNext() throws Exception {
        // Mock the route and capture the prehandler configured by addStaticHandlerFromClasspath
        Route route = mock(Route.class);
        ArgumentCaptor<Handler<RoutingContext>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
        when(route.handler(handlerCaptor.capture())).thenReturn(route);
        // Base points to this test package, so the file StaticHandlerUtilsTest.class is guaranteed to exist in the classpath.
        plugin.addStaticHandlerFromClasspath(route, "com/gentics/mesh/plugin/statichandler", null);

        Handler<RoutingContext> handler = handlerCaptor.getAllValues().get(0);

        // Simulate that the route matched "/static/*" and the request asked for "/static/StaticHandlerUtilsTest.class"
        Route currentRoute = mock(Route.class);
        // defining route
        when(currentRoute.getPath()).thenReturn("/static/*");
        RoutingContext rc = mock(RoutingContext.class);
        when(rc.currentRoute()).thenReturn(currentRoute);
        when(rc.mountPoint()).thenReturn(null);
        when(rc.normalisedPath()).thenReturn("/static/StaticHandlerUtilsTest.class");

        CountDownLatch nextLatch = new CountDownLatch(1);
        doAnswer(inv -> {
            nextLatch.countDown();
            return null;
        }).when(rc).next();

        CountDownLatch failLatch = new CountDownLatch(1);
        doAnswer(inv -> {
            failLatch.countDown();
            return null;
        }).when(rc).fail(404);

        handler.handle(rc);

        assertTrue("Expected rc.next() to be called", nextLatch.await(5, TimeUnit.SECONDS));
        assertFalse("Did not expect rc.fail(404) to be called", failLatch.await(5, TimeUnit.MILLISECONDS));

        verify(rc).next();
    }

    @After
    public void teardown() {
        if (vertx != null) {
            vertx.close();
        }
    }
}
