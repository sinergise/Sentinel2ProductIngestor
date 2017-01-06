package com.sinergise.sentinel.ingestor;

import static com.amazonaws.services.s3.internal.Constants.MB;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.sinergise.sentinel.ingestor.ProductIngestorSettings.SciHubCredentials;
import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.l1c.product.mapping.TileSequenceProvider;
import com.sinergise.sentinel.l1c.product.mapping.TileSequenceProviderAmazonS3;
import com.sinergise.sentinel.scihub.SciHubEntry;

public class ProductIngestor {

	private static final Logger logger = LoggerFactory.getLogger(ProductIngestor.class);
	
	private static final int MAX_CONCURRENT_CONNECTIONS_PER_SCIHUB_ACCOUNT = 2;
	
	// to ingest (ordered)
	private LinkedList<SciHubEntry> toIngest = new LinkedList<>();
	// to download (ordered)
	private LinkedList<SciHubEntry> toDownload = new LinkedList<>();
	// currently downloading
	private List<SciHubEntry> downloading = new ArrayList<>();
	// currently ingesting
	private List<SciHubEntry> ingesting = new ArrayList<>();

	// ingested products
	// TODO: cleanup older than a few days
	private LinkedList<SciHubEntry> ingested = new LinkedList<SciHubEntry>(); 
	
	
	public boolean isRunning = false;
	
	private ProductFeeder feeder;
	private AmazonS3 s3;
	private AmazonSNSClient snsClient;
	private TransferManager s3TransferManager;
	
	private TileSequenceProvider tileSequenceProvider;
	private ProductIngestorSettings settings;
	
	private ForkJoinPool s3UploadExecutorService;
	private ThreadPoolExecutor productIngestionTaskExecutor;
	
	private DataDownloader[] downloaders;
	
	public ProductIngestor() throws FileNotFoundException, IOException {
		
		settings = new ProductIngestorSettings(new File(System.getProperties().getProperty("config")));
		
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file!", e);
		}
		
		
		
		ClientConfiguration s3ClientConfig = new ClientConfiguration();
		s3 = new AmazonS3Client(credentials, s3ClientConfig);
		Region euCentral = Region.getRegion(Regions.EU_CENTRAL_1);
	    s3.setRegion(euCentral);
	    
		snsClient = new AmazonSNSClient(credentials);
		snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1));

	    
	    tileSequenceProvider = new TileSequenceProviderAmazonS3(s3, settings.getS3BucketName());
	    
	    TransferManagerConfiguration tmConfig = new TransferManagerConfiguration();
	    tmConfig.setMinimumUploadPartSize(50 * MB);
	    s3TransferManager = new TransferManager(s3); //uses separate pool, for multipart zip upload
	    s3TransferManager.setConfiguration(tmConfig);
	    
	    s3UploadExecutorService = new ForkJoinPool(100);

	}
	
	
	public void shutdown() {
		if (isRunning) {
			isRunning = false;
			getProductIngestionTaskExecutor().shutdown();
			logger.info("Waiting for running ingestor processes to finish!");
			System.out.println("Waiting for running ingestor processes to finish! No forceful termination please!");
			try {
				//ingestors should shutdown cleanly
				getProductIngestionTaskExecutor().awaitTermination(1, TimeUnit.HOURS);
			} catch (InterruptedException e) {
				logger.error("Clean shutdown of ingestors interrupted!", e);
				System.out.println("Clean shutdown of ingestors interrupted!");
				e.printStackTrace();
			}
		}
	}
	
	public void start() throws MalformedURLException {
		if (settings.getSciHubCredentialsList().size() == 0) {
			logger.error("No sciHubCredentials defined. Refusing to continue!");
			return;
		}
		
		logger.info("Using {} product processing threads!",settings.getProductProcessorThreadCount());
		
		isRunning = true;
		productIngestionTaskExecutor = new ThreadPoolExecutor(
				settings.getProductProcessorThreadCount(),
				settings.getProductProcessorThreadCount(),
				10, // time to wait before resizing pool
				TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10, true), new ThreadPoolExecutor.CallerRunsPolicy());


		
		int downloaderAccountsCount = settings.getSciHubCredentialsList().size();
		downloaders = new DataDownloader[downloaderAccountsCount];
		for (int i=0;i<downloaderAccountsCount;i++) {
			SciHubCredentials sciHubCredentials = settings.getSciHubCredentialsList().get(i);
			downloaders[i] = new DataDownloader(this, 
					 MAX_CONCURRENT_CONNECTIONS_PER_SCIHUB_ACCOUNT,
					 sciHubCredentials.getUsername(),
					 sciHubCredentials.getPassword());
			Thread downloaderThread = new Thread(downloaders[i]);
			downloaderThread.start();
		}
		
		
		feeder = new ProductFeeder(this);
		Thread feederThread = new Thread(feeder);
		feederThread.start();
	}
	

	public boolean isRunning() {
		return isRunning;
	}

	public AmazonS3 getS3Client() {
		return s3;
	}

	public AmazonSNSClient getSNSClient() {
		return snsClient;
	}
	
	public TileSequenceProvider getTileSequenceProvider() {
		return tileSequenceProvider;
	}

	public ForkJoinPool getS3UploadExecutorService() {
		return s3UploadExecutorService;
	}

	public ThreadPoolExecutor getProductIngestionTaskExecutor() {
		return productIngestionTaskExecutor;
	}

	public TransferManager getS3TransferManager() {
		return s3TransferManager;
	}

	public SciHubEntry getSciHubEntryToIngest() {
		synchronized (this) {
			if (!toIngest.isEmpty()) {
				SciHubEntry entry = toIngest.poll();
				ingesting.add(entry);
				return entry;
			}
			return null;
		}
	}

	public ProductIngestorSettings getSettings() {
		return settings;
	}

	public SciHubEntry getSciHubEntryToDownload() {
		synchronized (this) {
			if (!toDownload.isEmpty()) {
				SciHubEntry entry = toDownload.poll();
				downloading.add(entry);
				return entry;
			}
		}
		return null;
	}

	public void redownload(SciHubEntry sciHubEntry) {
		synchronized(this) {
			if (!(downloading.remove(sciHubEntry) || ingesting.remove(sciHubEntry))) {
				logger.error("Trying to re-download an entry that wasn't in the pipeline! Fishy..");
			}
			toDownload.addFirst(sciHubEntry);
		}
		
	}

	public void ingestSciHubEntry(SciHubEntry sciHubEntry) {
		synchronized (this) {
			logger.info("Product '{}' download successfull!", sciHubEntry);
			downloading.remove(sciHubEntry);
			toIngest.add(sciHubEntry);
		}
		synchronized (feeder) {
			feeder.notify();
		}
		
	}

	public void sciHubEntryIngested(SciHubEntry entry) {
		synchronized (this) {
			ingesting.remove(entry);
			ingested.add(entry);
			logger.info("Entry {} fully ingested to S3!", entry);
		}
	}
	
	
	private void downloadEntry(SciHubEntry entry) {
		synchronized (this) {
			toDownload.add(entry);
		}
	}
	
	public boolean addEntry(SciHubEntry entry) {
		synchronized (this) {
			if (ingested.contains(entry) || toIngest.contains(entry) || toDownload.contains(entry) ||
				downloading.contains(entry)	|| ingesting.contains(entry)) {
				logger.trace("Entry {} is already in the pipeline or already ingested.", entry);
				return false;
			}
		}
		logger.trace("Checking S3 for {}.", entry);
		if (existsInS3(entry)) {
			logger.trace("Entry {} already ingested to S3. Skipping....", entry);
			ingested.add(entry);
			return false;
		}
		//TODO check if there's a downloaded file and it's CRC is ok
		downloadEntry(entry);
		logger.info("Entry {} added for ingestion!", entry);
		return true;
	}

	public boolean existsInS3(SciHubEntry entry) {
		String productInfoPath = L1CProductConstants.buildS3ProductInfoPath(settings.getS3ProductPathPrefix(), entry.getProductTime(), entry.getName());
		try {
			logger.trace("Getting product info {} from s3", productInfoPath);
			S3Object s3Object = s3.getObject(settings.getS3BucketName(), productInfoPath);
			logger.trace("Getting product info {} from s3 done {}!", productInfoPath, s3Object!=null);			
			boolean exists= s3Object != null ? true : false;
			s3Object.close();
			return exists;
		} catch (Exception ex) {
			logger.trace("Getting product info {} from s3 failed!", productInfoPath);
			return false;				
		}
		
	}

}
