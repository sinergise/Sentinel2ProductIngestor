package com.sinergise.sentinel.ingestor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductIngestorSettings {
	private static final Logger logger = LoggerFactory.getLogger(ProductIngestorSettings.class);

	private File localDownloadedProductsLocation;
	private File localUnpackedProductsLocation;
	private String s3BucketName = "sentinel-pds-test";

	private long localDownloadedProductsStorageCleanupSizeThreshold;
	private double localDownloadedProductsStorageCleanupPercentage;
	
	private int productProcessorThreads;
	private String sciHubBaseUrl = "https://scihub.copernicus.eu";
	
	private String newProductSNSArn = null;
	private List<SciHubCredentials> sciHubCredentialsList = new ArrayList<>();
	
	public static class SciHubCredentials {
		private String username;
		private String password;

		public SciHubCredentials(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}
	}

	public ProductIngestorSettings(File propertyFile) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		logger.info("Using configuration file {}!", propertyFile);
		try (InputStream in = new FileInputStream(propertyFile)) {
			prop.load(in);
		}
		loadSettings(prop);
	}

	private void loadSettings(Properties settings) {
		localDownloadedProductsLocation = new File(settings.getProperty("ingestor.products.storage.downloaded.location"));
		localDownloadedProductsLocation.mkdirs();
		if (!(localDownloadedProductsLocation.exists() && localDownloadedProductsLocation.isDirectory())) {
			throw new RuntimeException(
					"No local downloaded products location:" + localDownloadedProductsLocation.getPath());
		}
		
		localDownloadedProductsStorageCleanupSizeThreshold = Long
				.parseLong(settings.getProperty("ingestor.products.storage.downloaded.cleanup.size"));
		localDownloadedProductsStorageCleanupPercentage = Double
				.parseDouble(settings.getProperty("ingestor.products.storage.downloaded.cleanup.percent"));


		localUnpackedProductsLocation = new File(settings.getProperty("ingestor.products.storage.unpacked.location"));
		localUnpackedProductsLocation.mkdirs();
		if (!(localUnpackedProductsLocation.exists() && localUnpackedProductsLocation.isDirectory())) {
			throw new RuntimeException(
					"No local unpacked products location found " + localUnpackedProductsLocation.getPath());
		}

		
		productProcessorThreads = Integer.parseInt(settings.getProperty("ingestor.productprocessor.threads", "2"));
		
		s3BucketName = settings.getProperty("ingestor.s3.bucket_name", s3BucketName);
		
		sciHubBaseUrl = settings.getProperty("ingestor.sciHub.baseUrl", sciHubBaseUrl);
		
		newProductSNSArn = settings.getProperty("ingestor.notifications.new_product.sns.arn", null);
		if (newProductSNSArn == null) {
			logger.warn("Notifications when new product is ingested won't be sent through SNS!");
		}

		for (int i=0;i<Integer.parseInt(settings.getProperty("ingestor.downloader.nAccounts"));i++) {
			sciHubCredentialsList.add(new SciHubCredentials(
					settings.getProperty("ingestor.downloader."+i+".username"),
					settings.getProperty("ingestor.downloader."+i+".password")));
		}
	}
	
	
	public int getProductProcessorThreadCount() {
		return productProcessorThreads;
	}
	
	public List<SciHubCredentials> getSciHubCredentialsList() {
		return sciHubCredentialsList;
	}
	
	public long getLocalProductStorageCleanupSizeThreshold() {
		return localDownloadedProductsStorageCleanupSizeThreshold;
	}

	public double getLocalProductStorageCleanupPercent() {
		return localDownloadedProductsStorageCleanupPercentage;
	}

	public File getLocalProductsStorage() {
		return localDownloadedProductsLocation;
	}

	public File getUnpackedProductsLocation() {
		return localUnpackedProductsLocation;
	}

	public File getS3ProductPathPrefix() {
		return new File("");
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public String getSciHubBaseUrl() {
		return sciHubBaseUrl;
	}
	
	public String getNewProductSNSArn() {
		return newProductSNSArn;
	}
}
