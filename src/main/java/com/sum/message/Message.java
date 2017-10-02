package com.sum.message;

public class Message {

	/**
	 * <h1>Message</h1>
	 * 
	 * <p>
	 * Message format.
	 * </p>
	 */
	private MessageType type;
	private Object body;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *            the message type.
	 * @param body
	 *            the message body.
	 */
	public Message(MessageType type, Object body) {
		this.type = type;
		this.body = body;
	}

	public MessageType getType() {
		return type;
	}

	private void setType(MessageType type) {
		this.type = type;
	}

	public Object getBody() {
		return body;
	}

	private void setBody(Object body) {
		this.body = body;
	}

	
}
