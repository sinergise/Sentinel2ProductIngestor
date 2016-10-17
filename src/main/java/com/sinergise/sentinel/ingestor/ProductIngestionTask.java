package com.sinergise.sentinel.ingestor;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.sns.model.PublishRequest;
import com.sinergise.sentinel.l1c.product.ProductTransformer;
import com.sinergise.sentinel.l1c.product.info.ProductInfo;
import com.sinergise.sentinel.l1c.product.mapping.SciHubProduct;
import com.sinergise.sentinel.scihub.SciHubEntry;
import com.sinergise.sentinel.util.ArchiveExtractor;
import com.sinergise.sentinel.util.FileUtils;

public class ProductIngestionTask implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ProductIngestionTask.class);
	
	private SciHubEntry entry;	
	private ProductIngestor ingestor;

	public ProductIngestionTask(ProductIngestor productIngestor, SciHubEntry entry) {
		this.ingestor = productIngestor;
		this.entry = entry;
	}
	
	private void removeDirectorySilently(File directory) {
		try {
			FileUtils.deleteRecursively(directory);
		} catch (IOException ex) {
			logger.error("Failed to remove {}!", ex);
		}
	}		
	
	@Override
	//TODO: improve error handling..
	public void run() {
		if (!ingestor.isRunning()) {
			logger.info("Ingestor has stopped. Refusing to run!");
			return;
		}
		
		ProductIngestorSettings piSettings = ingestor.getSettings();
		File archiveFile = entry.getArchiveFile();
		logger.info("Ingesting archive {}!", archiveFile);
		File unpackedArchive = null;
		try {
			ArchiveExtractor extractor = new ArchiveExtractor(archiveFile, piSettings.getUnpackedProductsLocation());
			logger.info("Extracting archive {}, product {}!", archiveFile, entry.getName());
			long extStart = System.currentTimeMillis();
			unpackedArchive = extractor.extract();
			logger.info("Extracted {} in {} seconds.", entry.getName(), ((System.currentTimeMillis()-extStart)/1000.0));					
		} catch (Exception ex) {
			logger.error("Error while unpacking archive file {}. Will retry download!", archiveFile, ex);
			removeDirectorySilently(unpackedArchive);
			ingestor.redownload(entry);
			return;
		}
		
	 	SciHubProduct p = new SciHubProduct(unpackedArchive, entry);
	   	ProductTransformer ul = new ProductTransformer(p, piSettings.getS3ProductPathPrefix(),
	   				ingestor.getS3Client(),
	   				piSettings.getS3BucketName(),
	   				ingestor.getTileSequenceProvider());
	   	
	   	if (ingestor.getS3UploadExecutorService().invoke(ul)) {
	   		if (ingestor.getSettings().getNewProductSNSArn() != null) {
	   			try {
			   		ProductInfo productInfo = ul.getProductInfo();		   		
			   		logger.info("Notifying SNS about new product {}", productInfo.getName());
					PublishRequest snsPublishRequest = new PublishRequest(
							ingestor.getSettings().getNewProductSNSArn(), 
							ul.getObjectMapper().writeValueAsString(productInfo),
							productInfo.getName());
					ingestor.getSNSClient().publish(snsPublishRequest);
	   			} catch (Exception ex) { 
	   				logger.error("Failed to send newProduct SNS notification!",ex);
	   			}
	   		}

	   		removeDirectorySilently(unpackedArchive);
	   		
	   		for (int i=0;i<50;i++) { // try 50 times then give up
	   			logger.info("Uploading zip archive file {} take #{}",entry.getName(), i);
		   		Upload upload = ingestor.getS3TransferManager()
		   				.upload(piSettings.getS3ZipBucketName(), entry.getName()+".zip", archiveFile);
		   		try {
		        	upload.waitForCompletion();
		        	ingestor.sciHubEntryIngested(entry);
		        	logger.info("Done uploading zip archive file {}", entry.getName());
				   	return;
		        } catch (AmazonClientException | InterruptedException ex) {
		        	logger.error("Failed to upload zip archive file {}", entry.getName(), ex);
		        }			   		
	   		}
	   		logger.error("Failed to upload zip archive {} too many times. Giving up..", entry.getName());
	   		return;
	   	}
		logger.error("Failed to ingesting product {} ", entry);
	}
}
