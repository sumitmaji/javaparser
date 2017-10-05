package com.sum.frontend.java.parser;

import com.sum.frontend.java.JavaParserTD;
import static com.sum.frontend.java.JavaTokenType.IDENTIFIER;
import static com.sum.frontend.java.JavaTokenType.DOT;

/**
 * Grammer: Identifier ('.' Identifier)* 
 */
public class QualifiedNameParser extends JavaParserTD {

    public QualifiedNameParser(JavaParserTD parent) {
        super(parent);
    }

    public void parse() throws Exception {
        consumeToken(IDENTIFIER);
        while (currentToken().getType() == DOT) {
            consumeToken(DOT);
            consumeToken(IDENTIFIER);
        }

    }
}
