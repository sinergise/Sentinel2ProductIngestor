package com.sinergise.sentinel.ingestor.resource;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ContextResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WebApp extends Application {
	public static class JsonObjectMapperProvider implements ContextResolver<ObjectMapper> {

		public JsonObjectMapperProvider() {
		}

		@Override
		public ObjectMapper getContext(Class<?> type) {
			return new ObjectMapper();
		}

	}

	@SafeVarargs
	public static <T, E extends T> Set<T> asSet(E... e) {
		Set<T> set = new LinkedHashSet<>(e.length);
		for (T o : e) {
			set.add(o);
		}
		return set;
	}

	private final Set<Class<?>> classes = asSet(JsonObjectMapperProvider.class, StatusResource.class);

	@Override
	public Set<Class<?>> getClasses() {
		return Collections.unmodifiableSet(classes);
	}

}