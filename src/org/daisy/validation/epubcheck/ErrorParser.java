package org.daisy.validation.epubcheck;

import java.util.List;

import org.daisy.validation.epubcheck.EpubcheckBackend.Issue;

class ErrorParser {

	interface State {
		/**
		 * Processes a line
		 * 
		 * @param line
		 */
		public void processLine(final String line);
	}

	private State currentState;

	private final NormalState normalState;

	final State fnfState;

	final State confErrorState;

	public ErrorParser(final String theEpubfile) {
		normalState = new NormalState(this, theEpubfile);
		currentState = normalState;

		fnfState = new FileNotFoundState();
		confErrorState = new ConfError();
	}

	public List<Issue> getIssues() {
		return normalState.getIssues();
	}

	public void processLine(final String line) {
		currentState.processLine(line);
	}

	void setCurrentState(State theCurrentState) {
		currentState = theCurrentState;
	}

	private class FileNotFoundState implements State {

		@Override
		public void processLine(final String line) {
			if (!line.matches("^\\s+.*")) {
				setCurrentState(normalState);
			}
		}
	}

	private class ConfError implements State {

		@Override
		public void processLine(final String line) {
			if (!line.matches("^\\s+.*") && !line.matches("^Caused by:.*")) {
				setCurrentState(normalState);
			}
		}
	}
}