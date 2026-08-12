package com.lmt.lib.bldt.internal;

import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/** 文字列リソース */
public class Texts {
	/** 不明キー指定時の代替文字列 */
	private static final String INSTEAD = "???";

	private static ResourceBundle sBundle = null;

	public static void setup(String baseName, Locale locale) throws IOException {
		try {
			var bundle = ResourceBundle.getBundle(baseName, locale);
			sBundle = bundle;
		} catch (MissingResourceException e) {
			throw new IOException(e);
		}
	}

	public static String get(String key) {
		try {
			return sBundle.getString(key);
		} catch (Exception e) {
			return INSTEAD;
		}
	}
}
