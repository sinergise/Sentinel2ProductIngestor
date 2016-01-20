package com.sinergise.sentinel.scihub.opensearch;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class OpenSearchEntry {

	private static class OpenSearchProperty<T> {
		@JsonProperty("content")
		protected T value;
		
		@JsonProperty("name")
		protected String name;
		
		public String getName() {
			return name;
		}
		
		public T getValue() {
			return value;
		}
	}
	
	@JsonProperty("summary")
	String summary;
	@JsonProperty("id")
	String id;
	@JsonProperty("title")
	String title;
	
	@JsonProperty("link")
	Link[] links;
	
	@JsonProperty("str")
	OpenSearchProperty<String> [] stringProperties;
	@JsonProperty("bool")
	OpenSearchProperty<Boolean> [] booleanProperties;
	@JsonProperty("date")
	OpenSearchProperty<Date> [] dateProperties;
	@JsonProperty("double")
	OpenSearchProperty<Double> [] doubleProperties;
	@JsonProperty("int")
	OpenSearchProperty<Integer> [] integerProperties;
	
	
	public String getId() {
		return id;
	}
	
	public String getTitle() {
		return title;
	}
	
	@Override
	public String toString() {
		return "id: "+id;
	}

	public Link [] getLinks() {
		return links;
	}

	public OpenSearchProperty<Date>[] getDateProperties() {
		return dateProperties;
	}

	public Date getIngestionDate() {
		return getDate("ingestiondate");
	}

	public String getSize() {
		return getString("size");
	}
	
	private String getString(String propertyName) {
		for (OpenSearchProperty<String> p:stringProperties) {
			if(propertyName.equals(p.getName())) {
				return p.getValue();
			}
		}
		return null;
	}
	private Date getDate(String propertyName) {
		for (OpenSearchProperty<Date> p:dateProperties) {
			if(propertyName.equals(p.getName())) {
				return p.getValue();
			}
		}
		return null;
	}
}
