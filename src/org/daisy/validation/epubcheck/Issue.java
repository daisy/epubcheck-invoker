package org.daisy.validation.epubcheck;

public final class Issue {
	public final String type;
	public final String file;
	public final int lineNo;
	public final int colNo;
	public final String txt;

	public Issue(final String theType, final String theFile,
			final int theLineNo, final int theColNo, final String theTxt) {
		type = theType;
		file = theFile;
		lineNo = theLineNo;
		colNo = theColNo;
		txt = theTxt;
	}

	public Issue(final String theType, final String theFile,
			final String theTxt) {
		this(theType, theFile, -1, -1, theTxt);
	}

	public String toString() {
		return "type:'"
				+ type
				+ "' file:'"
				+ file
				+ (lineNo != -1 ? "' (" + lineNo + "," + colNo + ") " : " ")
				+ "txt: '" + txt + "'";
	}
}