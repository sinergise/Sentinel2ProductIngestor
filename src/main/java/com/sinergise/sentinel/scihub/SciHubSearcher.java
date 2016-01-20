package com.sinergise.sentinel.scihub;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinergise.sentinel.scihub.opensearch.Link;
import com.sinergise.sentinel.scihub.opensearch.OpenSearchResult;

public class SciHubSearcher {

	private static final SimpleDateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	private URL apiUrl;
	private CloseableHttpAsyncClient httpClient;
	private ObjectMapper mapper;

	public SciHubSearcher(URL apiUrl, CloseableHttpAsyncClient httpClient ) {
		this.apiUrl = apiUrl;
		this.httpClient = httpClient;
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}
	
	public OpenSearchResult next(OpenSearchResult result) throws JsonParseException, JsonMappingException, UnsupportedOperationException, InterruptedException, ExecutionException, IOException, URISyntaxException {
		Link nextLink = result.getFeed().getLink("next");
		if (nextLink == null) return null;
		return search(fixUrl(nextLink.getHref()));
	}

	// fixes broken SciHub link URLs
	private String fixUrl(String urlString) throws MalformedURLException, URISyntaxException {
		urlString = urlString.replaceAll("/api/", "/");
		urlString+="&format=JSON";
		URL url = new URL(urlString);
		URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		url = uri.toURL();
		return url.toString();
	}
	
	public OpenSearchResult search(Date from, Date to, String productId, long offset, long count) throws InterruptedException, ExecutionException, UnsupportedOperationException, IOException {
		StringBuffer searchBuffer = new StringBuffer();
		searchBuffer.append(apiUrl).append("?").append("format=JSON");
		StringBuffer query = new StringBuffer();
		query.append("productType:S2MSI1C");
		if (from != null || to != null) {
			StringBuffer dateSpec = new StringBuffer();
			dateSpec.append("[");
			dateSpec.append(from == null ? "*" : SOLR_DATE_FORMAT.format(from));
			dateSpec.append(" TO ");
			dateSpec.append(to == null ? "*" : SOLR_DATE_FORMAT.format(to));
			dateSpec.append("]");

			query.append(" AND ");
			query.append("ingestionDate:").append(dateSpec);
		}
		if (productId != null) {
			query.append(" AND ");
			query.append("identifier:").append(productId);				
		}

		searchBuffer.append("&q=").append(URLEncoder.encode(query.toString(), "UTF-8"));
		searchBuffer.append("&start=").append(offset);
		searchBuffer.append("&rows=").append(count);
		return search(searchBuffer.toString());
	}
	
	private OpenSearchResult search (String requestUrl) throws InterruptedException, ExecutionException, JsonParseException, JsonMappingException, UnsupportedOperationException, IOException {
		HttpGet request = new HttpGet(requestUrl);			
		Future<HttpResponse> future = httpClient.execute(request, null);
		HttpResponse response = future.get();
		if (response.getStatusLine().getStatusCode()!=200) {
			throw new ClientProtocolException(response.getStatusLine().getReasonPhrase());
		}
		return  mapper.readValue(response.getEntity().getContent(), OpenSearchResult.class);
	}
}
