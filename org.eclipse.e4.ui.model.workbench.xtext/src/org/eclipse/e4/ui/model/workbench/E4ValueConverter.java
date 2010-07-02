package org.eclipse.e4.ui.model.workbench;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractNullSafeConverter;
import org.eclipse.xtext.parsetree.AbstractNode;
import org.eclipse.xtext.parsetree.LeafNode;
import org.eclipse.xtext.util.Strings;

public class E4ValueConverter extends DefaultTerminalConverters {
	protected class FQNConverter extends AbstractNullSafeConverter<String> {

		private Set<String> toEscape;

		public FQNConverter(String... exceptions) {
			toEscape = new HashSet<String>(getKeywords());
			toEscape.removeAll(Arrays.asList(exceptions));
		}

		@Override
		protected String internalToString(String value) {
			int current = value.indexOf('.');
			if (current < 0) {
				if (toEscape.contains(value))
					return "^" + value;
				return value;
			}
			int last = 0;
			boolean first = true;
			StringBuilder b = new StringBuilder(value.length());
			while (last < value.length()) {
				String segment = value.substring(last, current);
				if (first)
					first = false;
				else
					b.append(".");
				if (toEscape.contains(segment))
					b.append("^");
				b.append(segment);
				last = current + 1;
				current = value.indexOf('.', last);
				if (current < 0)
					current = value.length();
			}
			return b.toString();
		}

		@Override
		protected String internalToValue(String string, AbstractNode node) {
			StringBuffer b = new StringBuffer();
			for (LeafNode n : node.getLeafNodes())
				if (!n.isHidden()) {
					String t = n.getText();
					b.append(t.startsWith("^") ? t.substring(1) : t);
				}
			return b.toString();
		}
	}

	private Set<String> keywords = null;

	@ValueConverter(rule = "EString")
	public IValueConverter<String> EString() {
		return new AbstractNullSafeConverter<String>() {
			@Override
			protected String internalToString(String value) {
				return '"' + Strings.convertToJavaString(value, false) + '"';
			}

			@Override
			protected String internalToValue(String string, AbstractNode node) {
				try {
					if (string.startsWith("\"") && string.endsWith("\""))
						return Strings.convertFromJavaString(string.substring(
								1, string.length() - 1), false);
					else
						return string;
				} catch (IllegalArgumentException e) {
					throw new ValueConverterException(e.getMessage(), node, e);
				}
			}
		};
	}

	@ValueConverter(rule = "FQN")
	public IValueConverter<String> FQN() {
		return new FQNConverter();
	}

	private Set<String> getKeywords() {
		if (keywords == null)
			keywords = GrammarUtil.getAllKeywords(getGrammar());
		return keywords;
	}

	@ValueConverter(rule = "ImportFQN")
	public IValueConverter<String> ImportFQN() {
		return new FQNConverter();
	}

	@ValueConverter(rule = "KeySequence")
	public IValueConverter<String> KeySequence() {
		return new AbstractNullSafeConverter<String>() {
			@Override
			protected String internalToString(String value) {
				return '<' + Strings.convertToJavaString(value, false) + '>';
			}

			@Override
			protected String internalToValue(String string, AbstractNode node) {
				try {
					return string.substring(1, string.length() - 1);
				} catch (IllegalArgumentException e) {
					throw new ValueConverterException(e.getMessage(), node, e);
				}
			}
		};
	}
}
