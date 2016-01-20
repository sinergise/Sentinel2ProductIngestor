package com.sinergise.sentinel.ingestor;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinergise.sentinel.scihub.SciHubEntry;

public class DataDownloader implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(DataDownloader.class);
	
	private ProductIngestor ingestor;
	
	private Object latch = new Object();
	private static final long LOCK_TIMEOUT=10000;
	private final File downloadDirectory;
	private final int concurrency;
	private CloseableHttpAsyncClient httpClient;
	private String username;
	
	public DataDownloader(ProductIngestor productIngestor,			
			int concurrency, 
			String username, 
			String password) throws MalformedURLException {
		this.ingestor = productIngestor;
		this.downloadDirectory = ingestor.getSettings().getLocalProductsStorage();
		this.username = username;	
		this.concurrency = concurrency;
		httpClient = createHttpClient(new URL(ingestor.getSettings().getSciHubBaseUrl()), username, password);
	}
	
	

	private CloseableHttpAsyncClient createHttpClient(URL credentialsScopeUrl, String username, String password) {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(credentialsScopeUrl.getHost(), credentialsScopeUrl.getPort()),
				new UsernamePasswordCredentials(username, password));
		CloseableHttpAsyncClient httpClient = HttpAsyncClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		httpClient.start();
		return httpClient;
	}
	
			
	@Override
	public void run() {
		SciHubProductConsumer [] downloaders = new SciHubProductConsumer[concurrency];
		while(ingestor.isRunning()) {
			for (int i=0;i<downloaders.length;i++) {
				if (downloaders[i] == null) {
					SciHubEntry toDownloadEntry = ingestor.getSciHubEntryToDownload();
					if (toDownloadEntry != null) {
						downloaders[i] = new SciHubProductConsumer(downloadDirectory, toDownloadEntry, latch);
						downloaders[i].setAccountInfo(username);
						try {
							downloaders[i].download(httpClient);
						} catch (FileNotFoundException e) {
							logger.error("Failed to start download of {}!", toDownloadEntry, e);
							ingestor.redownload(toDownloadEntry);
						}
					}
				}
			}
			
			try {
				synchronized (latch) {
					latch.wait(LOCK_TIMEOUT);
				}
			} catch (InterruptedException e) {
				logger.warn("Lock was interrupted!", e);
			}
			
			boolean hadFailures = false;
			for (int i=0;i<downloaders.length;i++) {
				if (downloaders[i]==null) continue;
				if (downloaders[i].isFinished()) {
					ingestor.ingestSciHubEntry(downloaders[i].getSciHubEntry());
					downloaders[i] = null;
				} else {
					boolean isCanceled = false;
					if (downloaders[i].isStuck()) {
						logger.warn("Product id {} download is stuck. Canceling download...!", downloaders[i].getSciHubEntry().getId());
						isCanceled = downloaders[i].stopDownload();
					}
					
					if (downloaders[i].hasFailed() || isCanceled){
						hadFailures = true;
						logger.warn("Product id {} download has failed. AccountInfo: {} Retrying download..!",
								downloaders[i].getSciHubEntry().getId(), 
								username);
						ingestor.redownload(downloaders[i].getSciHubEntry());
						downloaders[i] = null;
					} 
				}
			}
			if (hadFailures) { // wait a bit so that other downloaders might pick up the product download (account switch)
				try {
					synchronized (latch) {
						latch.wait(5000);
					}
				} catch (InterruptedException e) {
				}
			}

			
		}			
	}
}

