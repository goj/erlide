package org.erlide.jinterface.backend.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class PreferencesUtils {

	private static final String SEP = ";";

	public static String packList(final Iterable<String> list) {
		return packList(list, SEP);
	}

	public static List<String> unpackList(final String string) {
		return unpackList(string, SEP);
	}

	public static String packArray(final String[] strs) {
		final StringBuilder result = new StringBuilder();
		for (final String s : strs) {
			if (s.length() > 0) {
				result.append(s).append(SEP);
			}
		}
		return result.toString();
	}

	public static String[] unpackArray(final String str) {
		return unpackList(str).toArray(new String[0]);
	}

	public static List<String> readFile(final String file) {
		final List<String> res = new ArrayList<String>();
		try {
			final BufferedReader reader = new BufferedReader(new FileReader(
					file));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.length() > 0) {
						res.add(line);
					}
				}
			} finally {
				reader.close();
			}
		} catch (final IOException e) {
		}
		return res;
	}

	private PreferencesUtils() {
	}

	public static String packList(final Iterable<String> list, String sep) {
		final StringBuilder result = new StringBuilder();
		for (final String s : list) {
			result.append(s).append(sep);
		}
		String r = result.length() == 0 ? "" : result.substring(0, result
				.length()
				- sep.length());
		return r;
	}

	public static List<String> unpackList(final String string, final String sep) {
		final String[] v = string.split(sep);
		final List<String> result = new ArrayList<String>();
		for (String s : v) {
			String ss = s.trim();
			if (ss.length() > 0) {
				result.add(ss);
			}
		}
		return result;
	}

}
