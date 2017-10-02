package com.sum.frontend.java.tokens;

import com.sum.frontend.Source;
import com.sum.frontend.java.JavaToken;
import com.sum.frontend.java.JavaTokenType;

import static com.sum.frontend.java.JavaTokenType.*;
public class JavaWordToken extends JavaToken {

	public JavaWordToken(Source source) throws Exception {
		super(source);
	}
	
	/**
	 * Extract Java word token from the source
	 */
	public void extract() throws Exception{
		StringBuilder textBuffer = new StringBuilder();
		char currentChar = currentChar();
		
		//Get the word character(letter or digit). The scanner has already determined
		//that the first character is a letter.
		while(Character.isLetterOrDigit(currentChar)){
			textBuffer.append(currentChar);
			currentChar = nextChar();
		}
		
		text = textBuffer.toString();
		
		//Is it a reserved word or identifier?
		type = (RESERVED_WORDS.contains(text.toLowerCase())) ? 
				JavaTokenType.valueOf(text.toUpperCase()) : //reserved word
					IDENTIFIER; //Identifier
	}

}
