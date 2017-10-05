package com.sum.frontend.java.parser;


import com.sum.frontend.java.JavaParserTD;
import static com.sum.frontend.java.JavaTokenType.PACKAGE;
import static com.sum.frontend.java.JavaTokenType.SEMICOLON;

/**
 *Grammer: 'package' qualifiedName ';' 
 */
public class PackageParser extends JavaParserTD{

	public PackageParser(JavaParserTD parent) {
		super(parent);
	}
	
	public void parse() throws Exception{
	    consumeToken(PACKAGE);
	    QualifiedNameParser parser = new QualifiedNameParser(this);
	    parser.parse();
	    consumeToken(SEMICOLON);
	}

}
