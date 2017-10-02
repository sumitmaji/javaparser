package com.sum.frontend.java;

import static com.sum.frontend.java.JavaErrorCode.*;
import static com.sum.frontend.java.JavaTokenType.*;

import com.sum.frontend.EofToken;
import com.sum.frontend.Parser;
import com.sum.frontend.Scanner;
import com.sum.frontend.Token;
import com.sum.frontend.TokenType;
import com.sum.message.Message;
import static com.sum.message.MessageType.*;

/**
 * <h1>JavaParserTD</h1>
 * 
 * <p>
 * The top-down Java parser.
 * </p>
 */
public class JavaParserTD extends Parser {
	/**
	 * Constructor.
	 * 
	 * @param scanner
	 *            the scanner to be used with this parser.
	 */
	public JavaParserTD(Scanner scanner) {
		super(scanner);
	}

	protected static JavaErrorHandler errorHandler = new JavaErrorHandler();

	/**
	 * Parse a Java source program and generate the symbol table and the
	 * intermediate code.
	 */
	public void parse() throws Exception {
		Token token;
		long startTime = System.currentTimeMillis();
		try {
			// Loop over each token until the end of file.
			while (!((token = nextToken()) instanceof EofToken)) {
				TokenType tokenType = token.getType();
				if (tokenType != ERROR) {
					// Format each token.
					sendMessage(new Message(TOKEN, new Object[] {
							token.getLineNumber(), token.getPosition(),
							tokenType, token.getText(), token.getValue() }));
				} else {
					errorHandler.flag(token,
							(JavaErrorCode) token.getValue(), this);
				}
			}
			// Send the parser summary message.
			float elapsedTime = (System.currentTimeMillis() - startTime) / 1000f;
			sendMessage(new Message(PARSER_SUMMARY, new Number[] {
					token.getLineNumber(), getErrorCount(), elapsedTime }));
		} catch (java.io.IOException ex) {
			errorHandler.abortTranslation(IO_ERROR, this);
		}
	}

	/**
	 * Return the number of syntax errors found by the parser.
	 * 
	 * @return the error count.
	 */
	public int getErrorCount() {
		return errorHandler.getErrorCount();
	}
}