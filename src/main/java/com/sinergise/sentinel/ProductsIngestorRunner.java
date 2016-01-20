package com.sinergise.sentinel;

import java.net.URL;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinergise.sentinel.ingestor.ProductIngestor;
import com.sinergise.sentinel.ingestor.ProductIngestorSettings.SciHubCredentials;
import com.sinergise.sentinel.scihub.SciHubEntry;
import com.sinergise.sentinel.scihub.SciHubSearcher;
import com.sinergise.sentinel.scihub.opensearch.OpenSearchResult;

import net.sf.sevenzipjbinding.SevenZip;

public class ProductsIngestorRunner {
	public static final Logger logger = LoggerFactory.getLogger(ProductsIngestorRunner.class);
	
	public static void main(String[] args) throws Exception {
		SevenZip.initSevenZipFromPlatformJAR();

		final ProductIngestor ingestor = new ProductIngestor();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() { 
		    	logger.info("Shutting down!");
		    	System.out.println("Shutting down..");
		    	ingestor.shutdown();
		     }
		 });

		
		SciHubCredentials sciHubCredentials = 
					ingestor.getSettings().getSciHubCredentialsList()
					.stream().findFirst().orElse(null);
		if (sciHubCredentials == null) {
			throw new RuntimeException("Could not find any SciHubCredentials to work with!");
		}

		// start ingestor
		ingestor.start();

		
		CloseableHttpAsyncClient hc = initializeHttpClient(
				new URL(ingestor.getSettings().getSciHubBaseUrl()),
				sciHubCredentials.getUsername(),
				sciHubCredentials.getPassword());

		SciHubSearcher shs = new SciHubSearcher(
				new URL(ingestor.getSettings().getSciHubBaseUrl()+"/dhus/search"), 
				hc);
		
		while (ingestor.isRunning()) {
			try {
				DateTime from = new DateTime().withTimeAtStartOfDay().minusDays(2);
				DateTime to = new DateTime().withTimeAtStartOfDay().plusDays(1);
				
				logger.info("Executing search between {} and {}!", from, to);
				OpenSearchResult osr = shs.search(from.toDate(), to.toDate(), null, 0, 1000);
				while (osr != null) {
					osr.getFeed().getEntries().forEach(e -> ingestor.addEntry(new SciHubEntry(e)));
					osr = shs.next(osr);
				}
				logger.info("Search done.");

			} catch (Exception ex) {
				logger.error("Error while searching!",ex);
			}
			try {
				Thread.sleep(2*60*1000);
			} catch (InterruptedException ex) {}
		}
	}
	
	
	private static CloseableHttpAsyncClient initializeHttpClient(URL credentialsScopeUrl, String username, String password) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(credentialsScopeUrl.getHost(), credentialsScopeUrl.getPort()),
				new UsernamePasswordCredentials(username, password));
		RequestConfig requestConfig =  RequestConfig.custom()
                .setSocketTimeout(20000)
                .setConnectTimeout(20000)
                .setConnectionRequestTimeout(20000)
                .build();
		CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom().setDefaultCredentialsProvider(credsProvider)
				.setDefaultRequestConfig(requestConfig)
				.build();
		httpClient.start();
		return httpClient;
	}

}