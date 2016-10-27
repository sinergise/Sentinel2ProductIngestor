package com.sinergise.sentinel.l1c.product.mapping;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;

import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProductTile;

public class S3ProductTile {


	private File baseDirectory;
	private File metadataFile;
	private File previewFile;
	private File[] imageFiles;
	private File[] qiFiles;
	private File[] auxFiles;
	private File tileInfoFile;
	
	private S3ProductDatastrip datastrip;
	private AbstractSciHubProductTile sciHubTile;
	
	private S3Product s3Product;
	
	public S3ProductTile(S3Product s3Product, AbstractSciHubProductTile sciHubTile, File s3TilesBase, TileSequenceProvider tileSequenceProvider) {
		this.s3Product = s3Product;
		this.sciHubTile = sciHubTile;
		

		datastrip = s3Product.getDatastrip(sciHubTile.getTileMetadata().getDatastripId());

				
		SimpleDateFormat s3BucketDateFormat = L1CProductConstants.getS3BucketDateFormat();
		
		File baseBeforeSequence = new File(s3TilesBase, sciHubTile.getUtmZone() 
				+ File.separator + sciHubTile.getLatitudeBand() 
				+ File.separator + sciHubTile.getGridSquare() 
				+ File.separator + s3BucketDateFormat.format(getSensingTime()));
		
		int tileSequence = tileSequenceProvider.getSequence(L1CProductConstants.getS3ObjectName(baseBeforeSequence), sciHubTile.getTileMetadata().getTileId());
		
		
		baseDirectory = new File(s3TilesBase, sciHubTile.getUtmZone() 
				+ File.separator + sciHubTile.getLatitudeBand() 
				+ File.separator + sciHubTile.getGridSquare() 
				+ File.separator + s3BucketDateFormat.format(getSensingTime()) 
				+ File.separator + tileSequence);

		tileInfoFile = new File(baseDirectory, "tileInfo.json");
		metadataFile = new File(baseDirectory, "metadata.xml");
		previewFile = new File(baseDirectory, "preview.jp2");

		imageFiles = new File[sciHubTile.getImageFiles().length];
		for (int i = 0; i < imageFiles.length; i++) {
			File sciHubImageFile = sciHubTile.getImageFiles()[i];
			Matcher imgMatcher = sciHubTile.getJp2BandFilePattern().matcher(sciHubImageFile.getName());
			if (!imgMatcher.matches()) {
				throw new IllegalStateException("Unrecognised image file:" + sciHubImageFile.getName());
			}
			imageFiles[i] = new File(baseDirectory, imgMatcher.group(3));
		}
		// QI
		File qiFileBase = new File(baseDirectory, "qi");
		qiFiles = new File[sciHubTile.getQiFiles().length];
		for (int i = 0; i < qiFiles.length; i++) {
			File sciHubQiFile = sciHubTile.getQiFiles()[i];
			Matcher mReport = sciHubTile.getQiReportFilePattern().matcher(sciHubQiFile.getName());
			Matcher mMask = sciHubTile.getMaskGMLFilePattern().matcher(sciHubQiFile.getName());
			if (mReport.matches()) {
				qiFiles[i] = new File(qiFileBase, mReport.group(2));
			} else if (mMask.matches()) {
				qiFiles[i] = new File(qiFileBase, mMask.group(1) + mMask.group(2) + mMask.group(3));
			} else {
				throw new IllegalStateException("Unrecognised QI file:" + sciHubQiFile.getName());
			}
		}
		// AUX
		File auxFileBase = new File(baseDirectory, "auxiliary");
		auxFiles = new File[sciHubTile.getAuxFiles().length];
		for (int i = 0; i < auxFiles.length; i++) {
			File auxFile = sciHubTile.getAuxFiles()[i];
			Matcher mAux = sciHubTile.getAuxFilePattern().matcher(auxFile.getName());
			if (!mAux.matches()) {
				throw new IllegalStateException("Unrecognised AUX file:" + auxFile.getName());
			}
			auxFiles[i] = new File(auxFileBase, mAux.group(1));
		}
	}

	public int getUtmZone() {
		return sciHubTile.getUtmZone();
	}
	
	public String getLatitudeBand() {
		return sciHubTile.getLatitudeBand();
	}
	
	public String getGridSquare() {
		return sciHubTile.getGridSquare();
	}
	
	public Date getSensingTime() {
		return sciHubTile.getTileMetadata().getSensingTime();
	}
	
	public File getMetadataFile() {
		return metadataFile;
	}

	public File[] getAuxFiles() {
		return auxFiles;
	}

	public File[] getQiFiles() {
		return qiFiles;
	}

	public File[] getImageFiles() {
		return imageFiles;
	}

	public File getPreviewFile() {
		return previewFile;
	}
	
	public File getTileInfoFile() {
		return tileInfoFile;
	}

	public File getBaseDirectory() {
		return baseDirectory;
	}
	
	public S3ProductDatastrip getDatastrip() {
		return datastrip;
	}

	public S3Product getS3Product() {
		return s3Product;
	}	
}
