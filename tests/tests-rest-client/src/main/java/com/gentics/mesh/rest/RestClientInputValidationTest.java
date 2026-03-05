package com.gentics.mesh.rest;

import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.impl.MeshRestOkHttpClientImpl;
import com.gentics.mesh.rest.client.method.AdminClientMethods;
import com.gentics.mesh.rest.client.method.AdminPluginClientMethods;
import com.gentics.mesh.rest.client.method.ApiInfoClientMethods;
import com.gentics.mesh.rest.client.method.AuthClientMethods;
import com.gentics.mesh.rest.client.method.BranchClientMethods;
import com.gentics.mesh.rest.client.method.EventbusClientMethods;
import com.gentics.mesh.rest.client.method.GenericHttpMethods;
import com.gentics.mesh.rest.client.method.GraphQLClientMethods;
import com.gentics.mesh.rest.client.method.GroupClientMethods;
import com.gentics.mesh.rest.client.method.HealthClientMethods;
import com.gentics.mesh.rest.client.method.JobClientMethods;
import com.gentics.mesh.rest.client.method.LanguageClientMethods;
import com.gentics.mesh.rest.client.method.LocalConfigMethods;
import com.gentics.mesh.rest.client.method.MicroschemaClientMethods;
import com.gentics.mesh.rest.client.method.NavRootClientMethods;
import com.gentics.mesh.rest.client.method.NavigationClientMethods;
import com.gentics.mesh.rest.client.method.NodeBinaryFieldClientMethods;
import com.gentics.mesh.rest.client.method.NodeClientMethods;
import com.gentics.mesh.rest.client.method.NodeS3BinaryFieldClientMethods;
import com.gentics.mesh.rest.client.method.ProjectClientMethods;
import com.gentics.mesh.rest.client.method.RoleClientMethods;
import com.gentics.mesh.rest.client.method.SchemaClientMethods;
import com.gentics.mesh.rest.client.method.SearchClientMethods;
import com.gentics.mesh.rest.client.method.TagClientMethods;
import com.gentics.mesh.rest.client.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.client.method.UserClientMethods;
import com.gentics.mesh.rest.client.method.UtilityClientMethods;
import com.gentics.mesh.rest.client.method.WebRootClientMethods;
import com.gentics.mesh.rest.client.method.WebRootFieldClientMethods;
import com.gentics.mesh.util.UUIDUtil;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

/**
 * Test cases for parameter validation on the MeshRestClient.
 */
@RunWith(value = Parameterized.class)
public class RestClientInputValidationTest {
	/**
	 * Methods, which are ignored
	 */
	protected final static Set<String> IGNORED_METHODS = Set.of("debugInfo", "assignLanguageToProject", "unassignLanguageFromProject");

	/**
	 * Parameter types, which have no extra validation
	 */
	protected final static Set<String> IGNORED_PARAMETER_TYPES = Set.of("ParameterProvider[]", "PagingParameters");

	/**
	 * Method parameters, which are ignored
	 */
	protected final static Set<Pair<String, String>> IGNORED_METHOD_PARAMETERS = Set.of(
			Pair.of("downloadBinaryField", "languageTag"), Pair.of("updateNodeBinaryFieldCheckStatus", "reason"),
			Pair.of("get", "path"), Pair.of("put", "path"), Pair.of("delete", "path"), Pair.of("post", "path"),
			Pair.of("deleteEmpty", "path"));

	/**
	 * Client instance
	 */
	private static MeshRestOkHttpClientImpl client;

	/**
	 * Scan Result
	 */
	private static ScanResult scanResult;

	@Parameters(name = "{index}: {0}, argument {2}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (Class<? extends Object> clazz : Arrays.asList(NodeClientMethods.class, TagClientMethods.class,
				ProjectClientMethods.class, TagFamilyClientMethods.class, WebRootClientMethods.class,
				SchemaClientMethods.class, GroupClientMethods.class, UserClientMethods.class, RoleClientMethods.class,
				AuthClientMethods.class, SearchClientMethods.class, AdminClientMethods.class,
				AdminPluginClientMethods.class, MicroschemaClientMethods.class, NodeBinaryFieldClientMethods.class,
				NodeS3BinaryFieldClientMethods.class, UtilityClientMethods.class, NavigationClientMethods.class,
				NavRootClientMethods.class, EventbusClientMethods.class, BranchClientMethods.class,
				ApiInfoClientMethods.class, GraphQLClientMethods.class, JobClientMethods.class,
				GenericHttpMethods.class, HealthClientMethods.class, LocalConfigMethods.class,
				WebRootFieldClientMethods.class, LanguageClientMethods.class)) {
			for (Method method : clazz.getMethods()) {
				// ignore methods that have no parameters
				if (method.getParameterCount() == 0) {
					continue;
				}
				// ignore some methods
				if (IGNORED_METHODS.contains(method.getName())) {
					continue;
				}
				String params = Stream.of(method.getParameterTypes()).map(paramClass -> paramClass.getSimpleName())
						.collect(Collectors.joining(","));

				for (java.lang.reflect.Parameter parameter : method.getParameters()) {
					// ignore some parameter types
					if (IGNORED_PARAMETER_TYPES.contains(parameter.getType().getSimpleName())) {
						continue;
					}
					// ignore some parmeters for methods
					if (IGNORED_METHOD_PARAMETERS.contains(Pair.of(method.getName(), parameter.getName()))) {
						continue;
					}
					data.add(new Object[] {"%s(%s)".formatted(method.getName(), params), method, parameter.getName()});
				}
			}
		}

		return data;
	}

	@BeforeClass
	public static void setupOnce() {
		client = new MeshRestOkHttpClientImpl(MeshRestClientConfig.newConfig().setHost("localhost").build());
		scanResult = new ClassGraph().enableAllInfo().scan();
	}

	@AfterClass
	public static void tearDownOnce() {
		if (scanResult != null) {
			scanResult.close();
			scanResult = null;
		}
	}

	/**
	 * Method description (for output)
	 */
	@Parameter(0)
	public String description;

	/**
	 * Tested method
	 */
	@Parameter(1)
	public Method method;

	/**
	 * Name of the parameter which might be filled with null or invalid values
	 */
	@Parameter(2)
	public String parameterName;

	/**
	 * Test calling the method with the given parameter filled with an invalid value
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@Test
	public void testInvalid() throws IllegalAccessException, InvocationTargetException {
		List<Object> arguments = Stream.of(method.getParameters()).map(parameter -> {
			if (Strings.CI.equals(parameter.getName(), parameterName)) {
				return getInvalidValue(parameter);
			} else {
				return getValidValue(parameter);
			}
		}).collect(Collectors.toList());

		try {
			method.invoke(client, arguments.toArray());
			fail("Invoking the method should have failed");
		} catch (Throwable t) {
			if (getCause(t) instanceof InvalidParameterException || getCause(t) instanceof NullPointerException
					|| getCause(t) instanceof IllegalArgumentException) {
				// all is well, as this is expected
			} else {
				throw t;
			}
		}
	}

	/**
	 * Test calling the method with the given parameter filled with null
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@Test
	public void testNull() throws IllegalAccessException, InvocationTargetException {
		List<Object> arguments = Stream.of(method.getParameters()).map(parameter -> {
			if (Strings.CI.equals(parameter.getName(), parameterName)) {
				return null;
			} else {
				return getValidValue(parameter);
			}
		}).collect(Collectors.toList());

		try {
			method.invoke(client, arguments.toArray());
			fail("Invoking the method should have failed");
		} catch (Throwable t) {
			if (getCause(t) instanceof InvalidParameterException || getCause(t) instanceof NullPointerException
					|| getCause(t) instanceof IllegalArgumentException) {
				// all is well, as this is expected
			} else {
				throw t;
			}
		}
	}

	/**
	 * Test calling the method with valid parameters
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	@Test
	public void testValid() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Object> arguments = Stream.of(method.getParameters()).map(parameter -> {
			return getValidValue(parameter);
		}).collect(Collectors.toList());

		method.invoke(client, arguments.toArray());
	}

	/**
	 * Get the root cause of the given throwable
	 * @param t throwable
	 * @return root cause
	 */
	protected Throwable getCause(Throwable t) {
		if (t.getCause() == null || t.getCause() == t) {
			return t;
		} else {
			return getCause(t.getCause());
		}
	}

	/**
	 * Get a valid value of the given parameter
	 * @param parameter parameter
	 * @return valid value
	 */
	protected Object getValidValue(java.lang.reflect.Parameter parameter) {
		String typeName = parameter.getType().getSimpleName();
		String name = parameter.getName();

		switch (typeName) {
		case "long":
			return 1L;
		case "boolean":
			return true;
		case "String":
			if (Strings.CI.contains(name, "uuid")) {
				// parameters with "uuid" in the name are expected to be filled with uuids
				return UUIDUtil.randomUUID();
			} else if (Strings.CI.contains(name, "path")) {
				// parameters with "path" in the name are expected to be filled with paths
				return "/bli/bla/blubb";
			} else {
				return "bla";
			}
		case "String[]":
			if (Strings.CI.contains(name, "path")) {
				// path segments
				return new String [] {"bli", "bla", "blubb"};
			} else {
				return new String [] {"bla"};
			}
		case "ParameterProvider[]":
			return new ParameterProvider[0];
		case "SchemaReference[]":
			return new SchemaReference[0];
		case "MicroschemaReference[]":
			return new MicroschemaReference[0];
		case "BinaryCheckStatus":
			return BinaryCheckStatus.ACCEPTED;
		case "InputStream":
			return new ByteArrayInputStream("bla".getBytes());
		case "GraphQLRequest":
			return new GraphQLRequest().setQuery("bla");
		case "Class":
			return Object.class;
		default:
			try {
				return getImplClass(parameter.getType()).getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				fail("Unknown parameter type %s".formatted(typeName));
			}
		}
		return null;
	}

	/**
	 * If the given class is an interface, get the first implementing class, which has a zero argument constructor
	 * or return the class otherwise
	 * @param clazz class
	 * @return implementing class or the class itself
	 */
	protected Class<?> getImplClass(Class<?> clazz) {
		if (clazz.isInterface()) {
			for (ClassInfo implClass : scanResult.getClassesImplementing(clazz)) {
				Optional<MethodInfo> zeroArgConstructor = implClass.getConstructorInfo().stream().filter(methodInfo -> methodInfo.getParameterInfo().length == 0).findFirst();
				if (zeroArgConstructor.isPresent()) {
					return implClass.loadClass();
				}
			}
			return clazz;
		} else {
			return clazz;
		}
	}

	/**
	 * Get an invalid value for the given parameter
	 * @param parameter parameter
	 * @return invalid value (might be null)
	 */
	protected Object getInvalidValue(java.lang.reflect.Parameter parameter) {
		String typeName = parameter.getType().getSimpleName();
		String name = parameter.getName();

		switch (typeName) {
		case "long":
			return -1L;
		case "String":
			if (Strings.CI.contains(name, "uuid")) {
				// this is no uuid
				return "bla";
			} else if (Strings.CI.contains(name, "path")) {
				// path is invalid, because it would traverse up
				return "/bli/../../blubb";
			} else {
				return null;
			}
		case "String[]":
			if (Strings.CI.contains(name, "path")) {
				// path segments are invalid, because the path would traverse up
				return new String [] {"bli", "..", "..", "blubb"};
			} else {
				return new String [] {null};
			}
		default:
			return null;
		}
	}
}
