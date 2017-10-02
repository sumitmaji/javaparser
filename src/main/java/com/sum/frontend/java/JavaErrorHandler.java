package com.sum.frontend.java;

import static com.sum.frontend.java.JavaErrorCode.*;
import static com.sum.frontend.java.JavaTokenType.*;
import static com.sum.message.MessageType.SYNTAX_ERROR;

import com.sum.frontend.Parser;
import com.sum.frontend.Token;
import com.sum.message.Message;

/**
 * 
 * @author pramit
 *Error handler Java syntax errors.
 */
public class JavaErrorHandler {

	private static final int MAX_ERRORS = 125;
	private static int errorCount = 0; //count of syntax errors
	
	/**
	 * flag an error in the source line
	 */
	public void flag(Token token, JavaErrorCode errorCode, Parser parser){
		//Notify the parsers listers
		parser.sendMessage(new Message(SYNTAX_ERROR,new Object[]{token.getLineNumber(),
				token.getPosition(), token.getText(), errorCode.toString()}));
		
		if(++errorCount > MAX_ERRORS)
			abortTranslation(TOO_MANY_ERRORS,parser);
	}
	
	/**
	 * Abort the translation
	 */
	public void abortTranslation(JavaErrorCode errorCode, Parser parser){
		//Notify the parsers listener and then abort
		String fatalText = "FATAL ERROR: "+errorCode.toString();
		parser.sendMessage(new Message(SYNTAX_ERROR, new Object[]{0,0,"",fatalText}));
		System.exit(errorCode.getStatus());
	}

	public static int getErrorCount() {
		return errorCount;
	}
}
