package com.koobe.common.data.domain;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQuery;


/**
 * The persistent class for the EpubConvertResult database table.
 * 
 */
@Entity
@NamedQuery(name="EpubConvertResult.findAll", query="SELECT e FROM EpubConvertResult e")
public class EpubConvertResult implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	private String guid;

	private String convertProcessErrorOutput;

	private String convertProcessExitValue;

	private String convertProcessOutput;

	private Integer copyFileElapsed;

	private Integer runConverterProcessElapsed;

	private String runningExMsg;

	private String runningHost;

	private String runningStatus;

	private Integer runUploaderElapsed;

	private String uploaderExMsg;

	private Boolean uploadSuccess;
	
	private Date runningTime;

	public Date getRunningTime() {
		return runningTime;
	}

	public void setRunningTime(Date runningTime) {
		this.runningTime = runningTime;
	}

	public EpubConvertResult() {
	}

	public String getGuid() {
		return this.guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getConvertProcessErrorOutput() {
		return this.convertProcessErrorOutput;
	}

	public void setConvertProcessErrorOutput(String convertProcessErrorOutput) {
		this.convertProcessErrorOutput = convertProcessErrorOutput;
	}

	public String getConvertProcessExitValue() {
		return this.convertProcessExitValue;
	}

	public void setConvertProcessExitValue(String convertProcessExitValue) {
		this.convertProcessExitValue = convertProcessExitValue;
	}

	public String getConvertProcessOutput() {
		return this.convertProcessOutput;
	}

	public void setConvertProcessOutput(String convertProcessOutput) {
		this.convertProcessOutput = convertProcessOutput;
	}

	public Integer getCopyFileElapsed() {
		return this.copyFileElapsed;
	}

	public void setCopyFileElapsed(Integer copyFileElapsed) {
		this.copyFileElapsed = copyFileElapsed;
	}

	public Integer getRunConverterProcessElapsed() {
		return this.runConverterProcessElapsed;
	}

	public void setRunConverterProcessElapsed(Integer runConverterProcessElapsed) {
		this.runConverterProcessElapsed = runConverterProcessElapsed;
	}

	public String getRunningExMsg() {
		return this.runningExMsg;
	}

	public void setRunningExMsg(String runningExMsg) {
		this.runningExMsg = runningExMsg;
	}

	public String getRunningHost() {
		return this.runningHost;
	}

	public void setRunningHost(String runningHost) {
		this.runningHost = runningHost;
	}

	public String getRunningStatus() {
		return this.runningStatus;
	}

	public void setRunningStatus(String runningStatus) {
		this.runningStatus = runningStatus;
	}

	public Integer getRunUploaderElapsed() {
		return this.runUploaderElapsed;
	}

	public void setRunUploaderElapsed(Integer runUploaderElapsed) {
		this.runUploaderElapsed = runUploaderElapsed;
	}

	public String getUploaderExMsg() {
		return this.uploaderExMsg;
	}

	public void setUploaderExMsg(String uploaderExMsg) {
		this.uploaderExMsg = uploaderExMsg;
	}

	public Boolean getUploadSuccess() {
		return this.uploadSuccess;
	}

	public void setUploadSuccess(Boolean uploadSuccess) {
		this.uploadSuccess = uploadSuccess;
	}

}