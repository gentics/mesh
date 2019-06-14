///**
// * Copyright 2004 - 2017 Syncleus, Inc.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
///**
// * This product currently only contains code developed by authors
// * of specific components, as identified by the source code files.
// *
// * Since product implements StAX API, it has dependencies to StAX API
// * classes.
// *
// * For additional credits (generally to people who reported problems)
// * see CREDITS file.
// */
//package com.syncleus.ferma.ext;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//
//import org.reflections.Reflections;
//
//import com.syncleus.ferma.annotations.GraphElement;
//
///**
// * Type cache which also provides resolving methods which cache the result.
// */
//public class ElementTypeClassCache {
//
//	private final Map<String, Class> classStringCache = new HashMap<>();
//	private String[] basePaths;
//
//	public ElementTypeClassCache(String... packagePaths) {
//		this.basePaths = packagePaths;
//	}
//
//	public Class forName(final String className) {
//		return this.classStringCache.computeIfAbsent(className, (key) -> {
//			for (String basePath : basePaths) {
//				Set<Class<?>> graphTypeClasses = new Reflections(basePath).getTypesAnnotatedWith(GraphElement.class);
//				for (Class<?> clazz : graphTypeClasses) {
//					if (clazz.getSimpleName().equals(key)) {
//						return clazz;
//					}
//				}
//			}
//			throw new IllegalStateException("The class {" + className + "} cannot be found for basePaths {" + Arrays.toString(basePaths) + "}");
//		});
//	}
//}
