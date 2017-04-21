package com.sinergise.sentinel.ingestor;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinergise.sentinel.scihub.SciHubEntry;
import com.sinergise.sentinel.util.FileUtils;

public class ProductFeeder implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ProductFeeder.class);

	private ProductIngestor ingestor;
	
	public ProductFeeder(ProductIngestor ingestor) {
		this.ingestor = ingestor;
	}

	private void cleanupLocalStorage() {
		try {
			logger.trace("Cleaning up local storage!");
			ProductIngestorSettings piSettings = ingestor.getSettings();
			long totalSize = FileUtils.getDirectorySize(piSettings.getLocalProductsStorage());
			if (totalSize > piSettings.getLocalProductStorageCleanupSizeThreshold()) {
				long toRemoveSize = (long) ((double) totalSize * piSettings.getLocalProductStorageCleanupPercent());
				logger.info("Local product storage size exceeds {} bytes. Will remove {} bytes.",
						piSettings.getLocalProductStorageCleanupSizeThreshold(), toRemoveSize);
				FileUtils.removeOldestFiles(piSettings.getLocalProductsStorage(), toRemoveSize);
			}
		} catch (IOException ex) {
			logger.error("Failed to cleanup local product storage.", ex);
			System.exit(1);
		} finally {
			logger.trace("Done cleaning up local storage!");
		}
	}

	@Override
	public void run() {
		while (ingestor.isRunning()) {
			cleanupLocalStorage();
			SciHubEntry entry = ingestor.getSciHubEntryToIngest();
			if (entry != null) {
				logger.info("Queueing {} for ingestion.", entry.getName());
				ingestor.getProductIngestionTaskExecutor().execute(new ProductIngestionTask(ingestor, entry));
			} else {
				synchronized (this) {
					try {
						wait(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		logger.info("Shutdown.");
	}
}
