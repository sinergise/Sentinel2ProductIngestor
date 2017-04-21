package com.sinergise.sentinel.ingestor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentDecoderChannel;
import org.apache.http.nio.FileContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinergise.sentinel.scihub.SciHubEntry;

public class SciHubProductConsumer extends AbstractAsyncResponseConsumer<File> {

	private static final Logger logger = LoggerFactory.getLogger(SciHubProductConsumer.class);
	
	public static final long TAIL_DISCARD_BYTES=1024*1024*1; // last MB
	private static final long DOWNLOAD_STUCK_CHECK_INTERVAL_MS = 60*1000; // 60 seconds
	private static final long DOWNLOAD_STUCK_CHECK_MINIMUM_DOWNLOADED_BYTES = 1024*100; // 100kb
	
	private final File file;
	private RandomAccessFile accessFile;

	private HttpResponse response;
	private ContentType contentType;
	private Header contentEncoding;
	private FileChannel fileChannel;
	private long fileStreamIdx = 0;;
	
	private final SciHubEntry sciHubEntry;
	private Object latch;
	private Future<File> fileFuture = null;
	private long downloadStartTS;
	
	private long downloadedBytes=0;
	private long downloadedBytesMarker=0;
	private long downloadedBytesMarkerTs;
	private String accountInfo;
	
	public SciHubProductConsumer(File targetDirectory, SciHubEntry sciHubEntry, Object lock) {
		super();
		this.latch=lock;
		this.sciHubEntry = sciHubEntry;
		file = new File(targetDirectory, sciHubEntry.getId() + ".zip");
		if (file.exists()) {
			fileStreamIdx = file.length() - TAIL_DISCARD_BYTES;
			fileStreamIdx = fileStreamIdx < 0 ? 0 : fileStreamIdx;
		}
	}

	
	public SciHubProductConsumer(File targetDirectory, SciHubProductConsumer downloader) {
		this (targetDirectory, downloader.sciHubEntry, downloader.latch);
	}


	public SciHubEntry getSciHubEntry() {
		return sciHubEntry;
	}
	
	public void download(CloseableHttpAsyncClient httpClient) throws FileNotFoundException {
		
		HttpGet getRequest = new HttpGet(sciHubEntry.getContentUrl());
		if (fileStreamIdx > 0) {
			getRequest.setHeader("Range", "bytes=" + fileStreamIdx + "-");
		}
		this.accessFile = new RandomAccessFile(this.file, "rw");
		downloadStartTS = System.currentTimeMillis();
		downloadedBytesMarkerTs = System.currentTimeMillis();
		fileFuture = httpClient.execute(HttpAsyncMethods.create(getRequest), this, null);
	}
	
	public boolean isStuck() {
		boolean stuck = false;
		if ((System.currentTimeMillis()-downloadedBytesMarkerTs)< DOWNLOAD_STUCK_CHECK_INTERVAL_MS) {
			return false;
		}
		if ((downloadedBytes - downloadedBytesMarker) < DOWNLOAD_STUCK_CHECK_MINIMUM_DOWNLOADED_BYTES) {
			stuck = true;
		}
		downloadedBytesMarker = downloadedBytes;
		downloadedBytesMarkerTs = System.currentTimeMillis();
		return stuck;
	}
	
	@Override
	protected void onResponseReceived(final HttpResponse response) {
		Header hContentLength = response.getFirstHeader("Content-Length");
		long expectedFileLength = Long.parseLong(hContentLength.getValue());
		logger.info("Downloading product id {} size {} MB!", sciHubEntry, expectedFileLength/(1024*1024));
		this.response = response;
	}

	@Override
	protected void onEntityEnclosed(final HttpEntity entity, final ContentType contentType) throws IOException {
		this.contentType = contentType;
		this.contentEncoding = entity.getContentEncoding();
		this.fileChannel = this.accessFile.getChannel();
	}

	@Override
	protected void onContentReceived(final ContentDecoder decoder, final IOControl ioctrl) throws IOException {
		Asserts.notNull(this.fileChannel, "File channel");
		final long transferred;
		if (decoder instanceof FileContentDecoder) {
			transferred = ((FileContentDecoder) decoder).transfer(this.fileChannel, this.fileStreamIdx, Integer.MAX_VALUE);
		} else {
			transferred = this.fileChannel.transferFrom(new ContentDecoderChannel(decoder), this.fileStreamIdx,
					Integer.MAX_VALUE);
		}
		if (transferred > 0) {
			this.fileStreamIdx += transferred;
			this.downloadedBytes += transferred;
		}
		
		if (decoder.isCompleted()) {
			this.fileChannel.close();
		}
	}

	protected File process(HttpResponse response, File file, ContentType contentType) throws Exception {
		
		int statusCode = response.getStatusLine().getStatusCode();
		if (!(statusCode == HttpStatus.SC_OK ||
			  statusCode == HttpStatus.SC_PARTIAL_CONTENT )) {
			logger.error("Download failed (accountInfo: {}), code: {} {}!", accountInfo, statusCode);
			throw new ClientProtocolException("Download failed: " + response.getStatusLine());
		}
		sciHubEntry.setArchiveFile(file);
		logger.info("Downloading of {} done!", sciHubEntry);
		if (latch!=null) {
			synchronized (latch) {
				latch.notify();							
			}
		}
		return file;
	}
	
	public boolean stopDownload() {
		boolean canceled = fileFuture.cancel(true);
		cancel();
		try {
			if (fileChannel!=null) {
				fileChannel.close();
			}
			if (accessFile!=null) {
				accessFile.close();
			}
		} catch (Exception ex) {
			logger.error("Failed to close file!", ex);
		}
		return canceled;
	}


	@Override
	protected File buildResult(final HttpContext context) throws Exception {
		final FileEntity entity = new FileEntity(this.file, this.contentType);
		entity.setContentEncoding(this.contentEncoding);
		this.response.setEntity(entity);
		return process(this.response, this.file, this.contentType);
	}

	@Override
	protected void releaseResources() {
		try {
			this.accessFile.close();
		} catch (final IOException ignore) {
		}
	}


	public boolean isFinished() {
		if (fileFuture == null || !fileFuture.isDone()) return false;
		if (fileFuture.isDone()) {
			try {
				fileFuture.get();
				return true;
			} catch (Exception ex) {}
		}
		return false;
	}


	public Exception getDownloadException() {
		if (!fileFuture.isDone()) 
			return null;
		try {
			fileFuture.get();
			return null;
		} catch (Exception ex) {
			return ex;
		}
	}
	
	public boolean hasFailed() {
		if (fileFuture == null || !fileFuture.isDone()) return false;
		if (fileFuture.isDone()) {
			try {
				fileFuture.get();
				return false;
			} catch (Exception ex) {
				logger.error("Downloading product id {} has failed!", sciHubEntry, ex);
				return true;
			}
		}
		return false;
	}


	public void setAccountInfo(String accountInfo) {
		this.accountInfo=accountInfo;
	}
}
