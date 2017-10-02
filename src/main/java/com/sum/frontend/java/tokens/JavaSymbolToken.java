package com.sum.frontend.java.tokens;

import com.sum.frontend.Source;
import com.sum.frontend.java.JavaToken;

import static com.sum.frontend.java.JavaErrorCode.INVALID_CHARACTER;
import static com.sum.frontend.java.JavaTokenType.ERROR;
import static com.sum.frontend.java.JavaTokenType.SPECIAL_SYMBOLS;

public class JavaSymbolToken extends JavaToken {

	public JavaSymbolToken(Source source) throws Exception {
		super(source);
	}

	public void extract() throws Exception {
		char currentChar = currentChar();
		text = Character.toString(currentChar);
		type = null;
		switch (currentChar) {
		// Single-character special symbols.
		case '+':
		case '-':
		case '*':
		case '/':
		case ',':
		case ';':
		case '\'':
		case '=':
		case '(':
		case ')':
		case '[':
		case ']':
		case '{':
		case '}':
		case '^': {
			nextChar(); // consume character
			break;
		}
			// : or :=
		case ':': {
			currentChar = nextChar(); // consume ':';
			if (currentChar == '=') {
				text += currentChar;
				nextChar(); // consume '='
			}
			break;
		}
			// < or <= or <>
		case '<': {
			currentChar = nextChar(); // consume '<';
			if (currentChar == '=') {
				text += currentChar;
				nextChar(); // consume '='
			} else if (currentChar == '>') {
				text += currentChar;
				nextChar(); // consume '>'
			}
			break;
		}
			// > or >=
		case '>': {
			currentChar = nextChar(); // consume '>';
			if (currentChar == '=') {
				text += currentChar;
				nextChar(); // consume '='
			}
			break;
		}
			// . or ..
		case '.': {
			currentChar = nextChar(); // consume '.';
			if (currentChar == '.') {
				text += currentChar;
				nextChar(); // consume '.'
			}
			break;
		}
		default: {
			nextChar(); // consume bad character
			type = ERROR;
			value = INVALID_CHARACTER;
		}
		}
		// Set the type if it wasn't an error.
		if (type == null) {
			type = SPECIAL_SYMBOLS.get(text);
		}
	}
}
