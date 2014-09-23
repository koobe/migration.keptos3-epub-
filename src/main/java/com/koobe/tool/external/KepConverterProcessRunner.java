package com.koobe.tool.external;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KepConverterProcessRunner {
	
	protected Logger log = LoggerFactory.getLogger(getClass());
	
	private String batchFilePath;
	
	private String kepFileFolder;
	
	private String epubFileFolder;

	private Boolean runProcessSuccess;
	
	private Integer exitValue;
	
	private String outputString;
	
	private String errorString;
	
	public KepConverterProcessRunner(String batchFilePath,
			String kepFileFolder, String epubFileFolder) {
		super();
		this.batchFilePath = batchFilePath;
		this.kepFileFolder = kepFileFolder;
		this.epubFileFolder = epubFileFolder;
	}

	public void start() {
		
		ProcessBuilder tProcessBuilder = new ProcessBuilder(batchFilePath, kepFileFolder, epubFileFolder);
		
		StringContainer tOutString = null;
		StringContainer tErrString = null;
		StreamPumperString outPumper = null;
		StreamPumperString errPumper = null;
		
		Integer tProcessExitValue = null;
		
		try {
			Process process = tProcessBuilder.start();
			
			tOutString = new StringContainer();
			tErrString = new StringContainer();
			
			outPumper = new StreamPumperString(process.getInputStream(), tOutString);
			errPumper = new StreamPumperString(process.getErrorStream(), tErrString);

			outPumper.start();
			errPumper.start();
			process.waitFor();
			outPumper.join();
			errPumper.join();
			
			tProcessExitValue = process.exitValue();
			
			runProcessSuccess = true;
		} catch (Exception e) {
			runProcessSuccess = false;
			e.printStackTrace();
		} finally {
			exitValue = tProcessExitValue;
			outputString = tOutString.getReturnValue();
			errorString = tErrString.getReturnValue();
		}
	}
	
	public Boolean getRunProcessSuccess() {
		return runProcessSuccess;
	}

	public void setRunProcessSuccess(Boolean runProcessSuccess) {
		this.runProcessSuccess = runProcessSuccess;
	}

	public Integer getExitValue() {
		return exitValue;
	}

	public void setExitValue(Integer exitValue) {
		this.exitValue = exitValue;
	}

	public String getOutputString() {
		return outputString;
	}

	public void setOutputString(String outputString) {
		this.outputString = outputString;
	}

	public String getErrorString() {
		return errorString;
	}

	public void setErrorString(String errorString) {
		this.errorString = errorString;
	}
	
	private class StreamPumperString extends Thread {
		private InputStream is;
		private StringContainer os;

		public StreamPumperString(InputStream is, StringContainer os) {
			this.is = is;
			this.os = os;
		}
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line;

				String returnValue = "";
				
				while ((line = br.readLine()) != null)
					returnValue = returnValue + System.getProperty("line.separator") + "\t" + line;
				
				os.setReturnValue(returnValue);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class StringContainer {
		
		private String returnValue;

		public String getReturnValue() {
			return returnValue;
		}
		public void setReturnValue(String returnValue) {
			this.returnValue = returnValue;
		}
	}
}
