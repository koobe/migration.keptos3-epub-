package com.koobe.tool.worker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.koobe.common.storage.AmazonS3Storage;

public class S3EpubUploader {
	
	protected Logger log = LoggerFactory.getLogger(getClass());
	
	private String guid;
	
	private File epubFile;
	
	private AmazonS3Storage storage;
	
	private String bucket = "koobecloudepub";
	
	private String keyPrefix = "books-2x/epub/unzip";
	
	private ExecutorService executor;
	
	List<FutureTask<Boolean>> futureTasks = new ArrayList<FutureTask<Boolean>>();
	
	private Boolean uploadSuccess = true;
	
	private String uploadExMsg;
	
	public S3EpubUploader(String guid, File epubFile, AmazonS3Storage storage) {
		super();
		this.guid = guid;
		this.epubFile = epubFile;
		this.storage = storage;
		
		executor = Executors.newFixedThreadPool(10, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});
	}

	public void start() {
		
		String bookKeyPrefix = keyPrefix + "/" + guid.substring(0, 1) + 
				"/" + guid.substring(1, 2) + 
				"/" + guid.substring(2, 3) + 
				"/" + guid + ".epub";
		
		log.info("[{}] upload book {}", guid, bookKeyPrefix);
		
		ZipFile zipFile = null;
		
		try {
				
			zipFile = new ZipFile(epubFile);
			
			for (Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zipFile.entries(); e.hasMoreElements();) {
				
				ZipEntry zipEntry = e.nextElement();
				
				if (!zipEntry.isDirectory()) {
					
					String entryName = zipEntry.getName();
					InputStream is = zipFile.getInputStream(zipEntry);
					
					String fileKey = bookKeyPrefix + "/" + entryName;
					
					ObjectMetadata objectMetadata = null;
					
					try {
						objectMetadata = storage.getObjectMetadata(bucket, fileKey);
					} catch (RuntimeException re) {}
					
					if (objectMetadata == null) {
						log.info("[{}] upload book content {}", guid, fileKey);
						uploadFileToS3(fileKey, is, zipEntry.getSize());
					} else {
						if (objectMetadata.getContentLength() != zipEntry.getSize()) {
							log.info("[{}] upload book content {}", guid, fileKey);
							uploadFileToS3(fileKey, is, zipEntry.getSize());
						}
					}
				}
			}
			
			while (true) {
				Thread.sleep(550);
				boolean isAllDone = true;
				
				for (FutureTask<Boolean> task : futureTasks) {
					if (!task.isDone()) {
						isAllDone = false;
						break;
					}
					Thread.sleep(50);
				}
				
				if (isAllDone) {
					break;
				}
			}
			
		} catch (Exception e) {
			uploadSuccess = false;
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			uploadExMsg = sw.toString();
		} finally {
			if (zipFile != null) {
				try {
					zipFile.close();
				} catch (IOException e) {}
			}
		}
	}
	
	public Boolean getUploadSuccess() {
		return uploadSuccess;
	}

	public void setUploadSuccess(Boolean uploadSuccess) {
		this.uploadSuccess = uploadSuccess;
	}

	public String getUploadExMsg() {
		return uploadExMsg;
	}

	public void setUploadExMsg(String uploadExMsg) {
		this.uploadExMsg = uploadExMsg;
	}

	protected void uploadFileToS3(String key, InputStream is, long size) {
		
		PutToS3Worker putToS3Worker = new PutToS3Worker(is, size, bucket, key);
		FutureTask<Boolean> futureTask = new FutureTask<Boolean>(putToS3Worker);
		futureTasks.add(futureTask);
		
		executor.submit(futureTask);
	}

	
	protected class PutToS3Worker implements Callable<Boolean> {
		
		private InputStream input;
		private Long size;
		private String bucket;
		private String key;

		public PutToS3Worker(InputStream input, Long size, String bucket, String key) {
			super();
			this.input = input;
			this.size = size;
			this.bucket = bucket;
			this.key = key;
		}

		public Boolean call() throws Exception {
			
			boolean result = false;
			
			try {
				
		        if (size  == -1) {
		        	storage.putObject(bucket, key, input);
		        } else {
		        	storage.putObject(bucket, key, input, size);
		        }
		       
		        result = true;
			} catch (Exception e) {
				uploadSuccess = false;
				e.printStackTrace();
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) { }
				}
				
				this.input = null;
				this.size = null;
				this.bucket = null;
				this.key = null;
			}

			return result;
		}
	}
}
