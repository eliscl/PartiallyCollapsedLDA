package cc.mallet.pipe;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import cc.mallet.types.Instance;

/**
 * Allows for configuring special classes of Unicode characters to keep
 * @author Leif Jonsson
 *
 */
public class KeepConnectorPunctuationTokenizerLarge extends SimpleTokenizerLarge {

	private static final long serialVersionUID = 1L;

	public KeepConnectorPunctuationTokenizerLarge(File stopfile) {
		super(stopfile);
	}
	
	public KeepConnectorPunctuationTokenizerLarge(int languageFlag) {
		super(languageFlag);
	}
	
	public KeepConnectorPunctuationTokenizerLarge(HashSet<String> stoplist) {
		super(stoplist);
	}
	
	public KeepConnectorPunctuationTokenizerLarge(File stopfile, int bufferSize) {
		super(stopfile, bufferSize);
	}
	
	public KeepConnectorPunctuationTokenizerLarge(int languageFlag, int bufferSize) {
		super(languageFlag, bufferSize);
	}
	
	public KeepConnectorPunctuationTokenizerLarge(HashSet<String> stoplist, int bufferSize) {
		super(stoplist, bufferSize);
	}
	
	@Override
	public KeepConnectorPunctuationTokenizerLarge deepClone() {
		return new KeepConnectorPunctuationTokenizerLarge((HashSet<String>) stoplist.clone());
	}
	
	public Instance pipe(Instance instance) {
		
		if (instance.getData() instanceof CharSequence) {
				
			CharSequence characters = (CharSequence) instance.getData();

			ArrayList<String> tokens = new ArrayList<String>();

			int[] tokenBuffer = new int[tokenBufferSize];
			int length = -1;

			// Using code points instead of chars allows us
			//  to support extended Unicode, and has no significant
			//  efficiency costs.
			
			int totalCodePoints = Character.codePointCount(characters, 0, characters.length());

			for (int i=0; i < totalCodePoints; i++) {

				int codePoint = Character.codePointAt(characters, i);
				int codePointType = Character.getType(codePoint);

				if (codePointType == Character.LOWERCASE_LETTER ||
					codePointType == Character.CONNECTOR_PUNCTUATION ||
					codePointType == Character.UPPERCASE_LETTER) {
					length++;
					tokenBuffer[length] = codePoint;
				}
				else if (codePointType == Character.SPACE_SEPARATOR ||
						 codePointType == Character.LINE_SEPARATOR ||
						 codePointType == Character.PARAGRAPH_SEPARATOR ||
						 codePointType == Character.END_PUNCTUATION ||
						 codePointType == Character.DASH_PUNCTUATION ||
						 codePointType == Character.START_PUNCTUATION ||
						 codePointType == Character.INITIAL_QUOTE_PUNCTUATION ||
						 codePointType == Character.FINAL_QUOTE_PUNCTUATION ||
						 codePointType == Character.OTHER_PUNCTUATION) {
					
					// Things that delimit words
					if (length != -1) {
						String token = new String(tokenBuffer, 0, length + 1);
						if (! stoplist.contains(token)) {
							tokens.add(token);
						}
						length = -1;
					}
				}
				else if (codePointType == Character.COMBINING_SPACING_MARK ||
						 codePointType == Character.ENCLOSING_MARK ||
						 codePointType == Character.NON_SPACING_MARK ||
						 codePointType == Character.TITLECASE_LETTER ||
						 codePointType == Character.MODIFIER_LETTER ||
						 codePointType == Character.OTHER_LETTER) {
					// Obscure things that are technically part of words.
					//  Marks are especially useful for Indic scripts.

					length++;
					tokenBuffer[length] = codePoint;
				}
				else {
					// Character.DECIMAL_DIGIT_NUMBER
					// Character.CONTROL
					// Character.MATH_SYMBOL
					//System.out.println("type " + codePointType);
				}
					
			}

			if (length != -1) {
				String token = new String(tokenBuffer, 0, length + 1);
				if (! stoplist.contains(token)) {
					tokens.add(token);
				}
			}

			instance.setData(tokens);
		}
		else {
			throw new IllegalArgumentException("Looking for a CharSequence, found a " + 
											   instance.getData().getClass());
		}
		
		return instance;
	}

	public int getTokenBufferSize() {
		return tokenBufferSize;
	}

	public void setTokenBufferSize(int tokenBufferSize) {
		this.tokenBufferSize = tokenBufferSize;
	}
	
}
