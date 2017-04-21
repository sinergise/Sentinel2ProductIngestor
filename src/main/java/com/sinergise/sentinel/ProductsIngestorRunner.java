package com.sinergise.sentinel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.internal.Mimetypes;
import com.sinergise.sentinel.ingestor.ProductIngestor;
import com.sinergise.sentinel.ingestor.ProductIngestorSettings.SciHubCredentials;
import com.sinergise.sentinel.ingestor.resource.StatusResource;
import com.sinergise.sentinel.ingestor.resource.WebApp;
import com.sinergise.sentinel.scihub.SciHubEntry;
import com.sinergise.sentinel.scihub.SciHubSearcher;
import com.sinergise.sentinel.scihub.opensearch.OpenSearchResult;

import net.sf.sevenzipjbinding.SevenZip;

public class ProductsIngestorRunner {
	public static final Logger logger = LoggerFactory.getLogger(ProductsIngestorRunner.class);

	
	private static void loadMimeTypes() throws IOException {	
		try (InputStream is = ProductsIngestorRunner.class.getResourceAsStream("/mime.types")) {
			Mimetypes.getInstance().loadAndReplaceMimetypes(is);
		} catch (Exception ex) {
			logger.error("Failed to load mime.types file!",ex);
			throw new RuntimeException("Failed to load mime.types!", ex);
		}
	}
	
	
	
	public static void main(String[] args) throws Exception {
		loadMimeTypes();
		Options options = new Options();
		options.addOption("f", "from", true, "from date (start of day)");
		options.addOption("t", "to", true, "to date (exclusive)");
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( options, args);
		
		DateTime fixedFromDate = null;
		DateTime fixedToDate =  null;
		if (cmd.hasOption("f") && cmd.hasOption("t")) {
			fixedFromDate = DateTime.parse(cmd.getOptionValue("f", "yyyy-MM-dd")).withTimeAtStartOfDay();
			fixedToDate = DateTime.parse(cmd.getOptionValue("t", "yyyy-MM-dd")).withTimeAtStartOfDay();
		}

		
		SevenZip.initSevenZipFromPlatformJAR();
		final ProductIngestor ingestor = ProductIngestor.instance();
		
		SciHubCredentials sciHubCredentials = 
					ingestor.getSettings().getSciHubCredentialsList()
					.stream().findFirst().orElse(null);
		if (sciHubCredentials == null) {
			throw new RuntimeException("Could not find any SciHubCredentials to work with!");
		}


		
		CloseableHttpAsyncClient hc = initializeHttpClient(
				new URL(ingestor.getSettings().getSciHubBaseUrl()),
				sciHubCredentials.getUsername(),
				sciHubCredentials.getPassword());

		String hub = System.getProperty("hub", "apihub");
		SciHubSearcher shs = new SciHubSearcher(
				new URL(ingestor.getSettings().getSciHubBaseUrl()+"/"+hub+"/search"), 
				hc);
		StatusResource.shs = shs;
		
		ResourceConfig config = ResourceConfig.forApplicationClass(WebApp.class);
		ServletHolder servlet = new ServletHolder(new ServletContainer(config));
		final Server server = new Server(6666);
		ServletContextHandler context = new ServletContextHandler(server, "/*");
		context.addServlet(servlet, "/*");
		server.start();
		server.dumpStdErr();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() { 
		    	logger.info("Shutting down!");
		    	System.out.println("Shutting down..");
		    	ingestor.shutdown();
		    	server.destroy();
		     }
		 });

		
		// start ingestor
		ingestor.start();
		while (ingestor.isRunning()) {
			try {
				DateTime from = fixedFromDate != null ? fixedFromDate : new DateTime().withTimeAtStartOfDay().minusDays(5);
				DateTime to = fixedToDate != null ? fixedToDate : new DateTime().withTimeAtStartOfDay().plusDays(1);
				
				logger.info("Executing search between {} and {}!", from, to);
				OpenSearchResult osr = shs.search(from.toDate(), to.toDate(), null, 0, 100);
				while (osr != null) {
					osr.getFeed().getEntries()
						.forEach(e -> {
						try {
							ingestor.addEntry(new SciHubEntry(e));
						} catch (Exception ex) {
							logger.error("Failed to add!", ex);
						}
						});
					osr = shs.next(osr);
				}
				logger.info("Search done.");

			} catch (Exception ex) {
				logger.error("Error while searching!",ex);
			}
			try {
				Thread.sleep(20*60*1000);
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
                .setCookieSpec("easy")
                .build();
		CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom().setDefaultCredentialsProvider(credsProvider)
				.setDefaultRequestConfig(requestConfig)
				.build();
		httpClient.start();
		return httpClient;
	}

}