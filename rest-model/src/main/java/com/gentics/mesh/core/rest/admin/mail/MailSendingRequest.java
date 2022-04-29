package com.gentics.mesh.core.rest.admin.mail;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import com.gentics.mesh.core.rest.common.AbstractResponse;

public class MailSendingRequest extends AbstractResponse {

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	@JsonProperty(required = true)
	@JsonPropertyDescription("The email of the sender")
	private String from;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The list of the emails of the receivers")
	private ArrayList to;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The list of the emails of the persons in cc")
	private ArrayList cc;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The list of the emails of the persons in bcc")
	private ArrayList bcc;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Da muss ich fragen")
	private String bounceAddress;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The subject of the email")
	private String subject;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The text of the email")
	private String text;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The text of the email")
	private String html;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The list of the attachments in the email")
	private ArrayList<MailAttachmentsRequest> attachments;

	public ArrayList getTo() {
		return to;
	}

	public void setTo(ArrayList to) {
		this.to = to;
	}

	public ArrayList getCc() {
		return cc;
	}

	public void setCc(ArrayList cc) {
		this.cc = cc;
	}

	public ArrayList getBcc() {
		return bcc;
	}

	public void setBcc(ArrayList bcc) {
		this.bcc = bcc;
	}

	public String getBounceAddress() {
		return bounceAddress;
	}

	public void setBounceAddress(String bounceAddress) {
		this.bounceAddress = bounceAddress;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public ArrayList<MailAttachmentsRequest> getAttachments() {
		return attachments;
	}

	public void setAttachments(ArrayList<MailAttachmentsRequest> attachments) {
		this.attachments = attachments;
	}
}
