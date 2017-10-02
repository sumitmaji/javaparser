package com.sum.intermediate.symtabimpl;

import java.util.ArrayList;
import java.util.HashMap;

import com.sum.intermediate.SymTab;
import com.sum.intermediate.SymTabEntry;
import com.sum.intermediate.SymTabKey;

/**
 * <h1>SymTabEntryImpl</h1>
 * 
 * <p>
 * An implementation of a symbol table entry.
 * </p>
 */
public class SymTabEntryImpl extends HashMap<SymTabKey, Object> implements
		SymTabEntry {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3069287051309193326L;
	private String name; // entry name
	private SymTab symTab; // parent symbol table
	private ArrayList<Integer> lineNumbers; // source line numbers

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            the name of the entry.
	 * @param symTab
	 *            the symbol table that contains this entry.
	 */
	public SymTabEntryImpl(String name, SymTab symTab) {
		this.name = name;
		this.symTab = symTab;
		this.lineNumbers = new ArrayList<Integer>();
	}

	/**
	 * Append a source line number to the entry.
	 * 
	 * @param lineNumber
	 *            the line number to append.
	 */
	public void appendLineNumber(int lineNumber) {
		lineNumbers.add(lineNumber);
	}

	/**
	 * Set an attribute of the entry.
	 * 
	 * @param key
	 *            the attribute key.
	 * @param value
	 *            the attribute value.
	 */
	public void setAttribute(SymTabKey key, Object value) {
		put(key, value);
	}

	/**
	 * Get the value of an attribute of the entry.
	 * 
	 * @param key
	 *            the attribute key.
	 * @return the attribute value.
	 */
	public Object getAttribute(SymTabKey key) {
		return get(key);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public SymTab getSymTab() {
		return symTab;
	}

	@Override
	public ArrayList<Integer> getLineNumbers() {
		return lineNumbers;
	}
}