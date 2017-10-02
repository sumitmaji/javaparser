package com.sum.frontend;

import com.sum.intermediate.ICode;
import com.sum.intermediate.SymTab;
import com.sum.message.Message;
import com.sum.message.MessageHandler;
import com.sum.message.MessageListener;
import com.sum.message.MessageProducer;

/**
 * <h1>Parser</h1>
 * 
 * <p>
 * A language-independent framework class. This abstract parser class will be
 * implemented by language-specific subclasses.
 * </p>
 */
public abstract class Parser implements MessageProducer {
	protected static SymTab symTab; // generated symbol table
	protected static MessageHandler messageHandler; // message handler delegate
	static {
		symTab = null;
		messageHandler = new MessageHandler();
	}
	protected Scanner scanner; // scanner used with this parser
	protected ICode iCode; // intermediate code generated by this parser

	/**
	 * Constructor.
	 * 
	 * @param scanner
	 *            the scanner to be used with this parser.
	 */
	protected Parser(Scanner scanner) {
		this.scanner = scanner;
		this.iCode = null;
	}

	/**
	 * Parse a source program and generate the intermediate code and the symbol
	 * table. To be implemented by a language-specific parser subclass.
	 * 
	 * @throws Exception
	 *             if an error occurred.
	 */
	public abstract void parse() throws Exception;

	/**
	 * Return the number of syntax errors found by the parser. To be implemented
	 * by a language-specific parser subclass.
	 * 
	 * @return the error count.
	 */

	public abstract int getErrorCount();

	/**
	 * Call the scanner's currentToken() method.
	 * 
	 * @return the current token.
	 */
	public Token currentToken() {
		return scanner.currentToken();
	}

	/**
	 * Call the scanner's nextToken() method.
	 * 
	 * @return the next token.
	 * @throws Exception
	 *             if an error occurred.
	 */
	public Token nextToken() throws Exception {
		return scanner.nextToken();
	}

	/**
	 * Add a parser message listener.
	 * 
	 * @param listener
	 *            the message listener to add.
	 */
	public void addMessageListener(MessageListener listener) {
		messageHandler.addListener(listener);
	}

	/**
	 * Remove a parser message listener.
	 * 
	 * @param listener
	 *            the message listener to remove.
	 */
	public void removeMessageListener(MessageListener listener) {
		messageHandler.removeListener(listener);
	}

	/**
	 * Notify listeners after setting the message.
	 * 
	 * @param message
	 *            the message to set.
	 */
	public void sendMessage(Message message) {
		messageHandler.sendMessage(message);
	}

	public static SymTab getSymTab() {
		return symTab;
	}

	public static void setSymTab(SymTab symTab) {
		Parser.symTab = symTab;
	}

	public ICode getICode() {
		return iCode;
	}

	public void setICode(ICode iCode) {
		this.iCode = iCode;
	}
	
	
}