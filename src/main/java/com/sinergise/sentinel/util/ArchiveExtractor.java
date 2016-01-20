package com.sinergise.sentinel.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

public class ArchiveExtractor {
	private static final Logger logger = LoggerFactory.getLogger(ArchiveExtractor.class);
	
	private File archiveFile;
	private File destination;
	
	

	public class ExtractCallback implements IArchiveExtractCallback {
		private int index;
		private IInArchive inArchive;
		private BufferedOutputStream bos ;

		public ExtractCallback(IInArchive inArchive) {
			this.inArchive = inArchive;
		}

		public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
			this.index = index;
			if (extractAskMode != ExtractAskMode.EXTRACT) {
				return null;
			}
			return new ISequentialOutStream() {

				public int write(byte[] data) throws SevenZipException {
					try {
						bos.write(data, 0, data.length);
					} catch (IOException e) {
						new SevenZipException(e);
					}
					return data.length; // Return amount of proceed data
				}
			};
		}

		public void prepareOperation(ExtractAskMode extractAskMode) throws SevenZipException {
			try {
				bos = new BufferedOutputStream(
						new FileOutputStream(new File(destination, inArchive.getProperty(index, PropID.PATH).toString())));
			} catch (FileNotFoundException e) {
				throw new SevenZipException(e);
			}
		}

		public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
			if (bos!=null) {
				try {
					bos.close();
				} catch (IOException e) {
					throw new SevenZipException(e);
				}
			}
			if (extractOperationResult != ExtractOperationResult.OK) {
				throw new SevenZipException("Extraction error");
			}
		}

		public void setCompleted(long completeValue) throws SevenZipException {
		}

		public void setTotal(long total) throws SevenZipException {
		}

	}
	
	
	
	public ArchiveExtractor (File archiveFile, File destinationDirectory) {
		this.destination = destinationDirectory;
		this.archiveFile = archiveFile;
	}
	
	public File extract() throws IOException {
		RandomAccessFile randomAccessFile = null;
		IInArchive inArchive = null;
		try {
			randomAccessFile = new RandomAccessFile(archiveFile, "r");
			inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile));
			if (inArchive.getNumberOfItems()== 0) {
				logger.error("Archive {} seems to be empty!", archiveFile);
				throw new IllegalStateException("Empty archive!");
			}
			if (!Boolean.TRUE.equals(inArchive.getProperty(0, PropID.IS_FOLDER))) {
				logger.error("Archive {} doesn't contain a folder. Not a S2 product archive?", archiveFile);
				throw new IllegalStateException("Illegal archive!");
			}
			File productDirectory = new File(destination, inArchive.getProperty(0, PropID.PATH).toString());
			if (productDirectory.exists()) {
				FileUtils.deleteRecursively(productDirectory);
			}
			
			int count = inArchive.getNumberOfItems();
			List<Integer> itemsToExtract = new ArrayList<Integer>();
			for (int i = 0; i < count; i++) {
				if (Boolean.TRUE.equals(inArchive.getProperty(i, PropID.IS_FOLDER))) {
					File folder = new File(destination, inArchive.getProperty(i, PropID.PATH).toString());
					folder.mkdirs();
				} else {
					itemsToExtract.add(Integer.valueOf(i));
				}
			}
			
			int[] items = itemsToExtract.stream().mapToInt(i -> i).toArray();			
			inArchive.extract(items, false, new ExtractCallback(inArchive));
			return productDirectory;
		} finally {
			if (inArchive != null) {
				try {
					inArchive.close();
				} catch (SevenZipException e) {
					logger.error("Error closing archive: ", e);
				}
			}
			if (randomAccessFile != null) {
				try {
					randomAccessFile.close();
				} catch (IOException e) {
					logger.error("Error closing file: ", e);
				}
			}
		}
	}
}
