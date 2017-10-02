package com.sum.frontend.java.tokens;

import com.sum.frontend.Source;
import com.sum.frontend.java.JavaErrorCode;
import com.sum.frontend.java.JavaToken;

import static com.sum.frontend.java.JavaTokenType.ERROR;
public class JavaErrorToken extends JavaToken{

	public JavaErrorToken(Source source, JavaErrorCode errorCode, String tokenText) throws Exception{
		super(source);
		this.text = tokenText;
		this.type = ERROR;
		this.value = errorCode;
	}
	
	/**
	 * Do nothing. Do not consume any character.
	 */
	protected void extract() throws Exception{
	}
}
