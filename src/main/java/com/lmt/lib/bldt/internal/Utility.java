package com.lmt.lib.bldt.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.json.JSONObject;

/**
 * LDTライブラリ向けのユーティリティクラス
 *
 * @hidden
 */
public class Utility {
	/** IDの正規表現パターン */
	public static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
	/** MD5の正規表現パターン */
	public static final Pattern MD5_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");
	/** SHA-256の正規表現パターン */
	public static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

	/** 16進数文字配列 */
	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	/**
	 * IDが有効かどうか判定
	 * @param str 文字列
	 * @return 有効であれば true
	 */
	public static boolean isIdValid(String str) {
		return ID_PATTERN.matcher(str).matches();
	}

	/**
	 * MD5が有効かどうか判定
	 * @param str 文字列
	 * @return 有効であれば true
	 */
	public static boolean isMd5(String str) {
		return MD5_PATTERN.matcher(str).matches();
	}

	/**
	 * SHA-256が有効かどうか判定
	 * @param str 文字列
	 * @return 有効であれば true
	 */
	public static boolean isSha256(String str) {
		return SHA256_PATTERN.matcher(str).matches();
	}

	/**
	 * ハッシュ値の正規化(大文字が混じっている場合は小文字へ、nullの場合はnull)。構文チェックなし。
	 * @param hash ハッシュ値
	 * @return 正規化後のハッシュ値
	 */
	public static String normalizeHash(String hash) {
		return Objects.isNull(hash) ? null : hash.toLowerCase(Locale.US);
	}

	/**
	 * バイト配列を文字列へ変換
	 * @param bytes バイト配列
	 * @return バイト配列の文字列表現
	 */
	public static String byteArrayToString(byte[] bytes) {
		var sb = new StringBuilder(bytes.length * 2);
		for (var b : bytes) {
			sb.append(HEX_CHARS[(b >> 4) & 0x0f]);
			sb.append(HEX_CHARS[b & 0x0f]);
		}
		return sb.toString();
	}

	/**
	 * JSON項目から任意項目のハッシュ値を取り出す
	 * @param maybeStr 文字列表現のハッシュ値
	 * @param tester ハッシュ値のテスター
	 * @param fnError ハッシュ値不正の場合に実行する関数
	 * @return 正常なハッシュ値を示す場合、maybeStr、そうでなければ null
	 */
	public static String optionalJsonHash(String maybeStr, Predicate<String> tester, Consumer<String> fnError) {
		if (isJsonNull(maybeStr) || maybeStr.isEmpty()) {
			return null;
		}
		if (!tester.test(maybeStr)) {
			fnError.accept(maybeStr);
			return null;
		}
		return maybeStr;
	}

	/**
	 * JSON項目から任意項目のURLを取り出す
	 * @param url URL文字列
	 * @param fnError URL不正の場合に実行する関数
	 * @return 正常なURLを示す場合はURLオブジェクト、そうでなければ null
	 */
	public static URL optionalJsonUrl(String url, Consumer<String> fnError) {
		try {
			// null, 空文字の時はnull、それ以外は正規のURL解析を実施
			return (isJsonNull(url) || url.isEmpty()) ? null : new URL(url);
		} catch (MalformedURLException e) {
			// 不正なURLの場合は無かったことにする
			fnError.accept(url);
			return null;
		}
	}

	/**
	 * 指定値、またはJSONのNULL値を返す
	 * @param <I> JSON項目から取り出した値のデータ型
	 * @param input JSON項目、または null
	 * @param converter JSON項目のコンバータ
	 * @return 指定値が null の場合、JSONのNULL値、そうでなければコンバータで変換した値
	 */
	public static <I> Object valueOrJsonNull(I input, Function<I, Object> converter) {
		return Objects.isNull(input) ? JSONObject.NULL : converter.apply(input);
	}

	/**
	 * 指定値が null かどうかの判定(単純な null またはJSONのNULL値)
	 * @param obj 判定対象のオブジェクト
	 * @return obj が null または JSONのNULL値の場合 true
	 */
	public static boolean isJsonNull(Object obj) {
		return Objects.isNull(obj) || (obj == JSONObject.NULL);
	}
}
