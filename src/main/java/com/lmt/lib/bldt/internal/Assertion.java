package com.lmt.lib.bldt.internal;

/**
 * Assertionクラスでは、アサーションの処理をとりまとめる。
 * 当クラスは外部パッケージへは非公開であり、ライブラリ特有の必要な処理がまとめられている。
 * 各クラスは当クラスを静的インポートし、必要な各種メソッドを呼び出す。
 * アサーションに失敗した場合、メソッドごとに定められた例外をスローする。これらの例外は基本的には
 * RuntimeExceptionを継承した例外となっている。
 *
 * @hidden
 */
public class Assertion {
	/**
	 * 引数の汎用アサーション
	 * @param success 評価式の結果
	 * @param msgFormat アサーション失敗時のメッセージ書式
	 * @param args メッセージ引数
	 * @throws IllegalArgumentException アサーションに失敗した
	 */
	public static void assertArg(boolean success, String msgFormat, Object...args) {
		if (!success) {
			throw new IllegalArgumentException(String.format(msgFormat, args));
		}
	}

	/**
	 * 引数の汎用アサーション
	 * @param success 評価式の結果
	 * @param msgFormat アサーション失敗時のメッセージ書式
	 * @param arg1 メッセージ引数1
	 * @throws IllegalArgumentException アサーションに失敗した
	 */
	public static void assertArg(boolean success, String msgFormat, int arg1) {
		if (!success) {
			throw new IllegalArgumentException(String.format(msgFormat, arg1));
		}
	}

	/**
	 * 引数の汎用アサーション
	 * @param success 評価式の結果
	 * @param msgFormat アサーション失敗時のメッセージ書式
	 * @param arg1 メッセージ引数1
	 * @param arg2 メッセージ引数2
	 * @throws IllegalArgumentException アサーションに失敗した
	 */
	public static void assertArg(boolean success, String msgFormat, int arg1, int arg2) {
		if (!success) {
			throw new IllegalArgumentException(String.format(msgFormat, arg1, arg2));
		}
	}

	/**
	 * 引数の汎用アサーション
	 * @param success 評価式の結果
	 * @param msgFormat アサーション失敗時のメッセージ書式
	 * @param arg1 メッセージ引数1
	 * @param arg2 メッセージ引数2
	 * @param arg3 メッセージ引数3
	 * @throws IllegalArgumentException アサーションに失敗した
	 */
	public static void assertArg(boolean success, String msgFormat, int arg1, int arg2, int arg3) {
		if (!success) {
			throw new IllegalArgumentException(String.format(msgFormat, arg1, arg2, arg3));
		}
	}

	/**
	 * 引数がnullではないことをテストするアサーション
	 * @param arg nullチェックを行う引数
	 * @param argName 引数の名前
	 * @throws NullPointerException 引数がnullだった
	 */
	public static void assertArgNotNull(Object arg, String argName) {
		if (arg == null) {
			throw new NullPointerException(String.format("Argument '%s' is null.", argName));
		}
	}

	/**
	 * 引数の値の範囲をテストするアサーション
	 * @param value テストする引数の値
	 * @param min 許容最小値
	 * @param max 許容最大値
	 * @param valueName 引数の名前
	 * @throws IllegalArgumentException 引数の値が許容範囲外だった
	 */
	public static void assertArgRange(int value, int min, int max, String valueName) {
		if ((value < min) || (value > max)) assertArgRangeFail(value, min, max, valueName);
	}

	/**
	 * 引数の値の範囲をテストするアサーション
	 * @param value テストする引数の値
	 * @param min 許容最小値
	 * @param max 許容最大値
	 * @param valueName 引数の名前
	 * @throws IllegalArgumentException 引数の値が許容範囲外だった
	 */
	public static void assertArgRange(long value, long min, long max, String valueName) {
		if ((value < min) || (value > max)) assertArgRangeFail(value, min, max, valueName);
	}

	/**
	 * 引数の値の範囲をテストするアサーション
	 * @param value テストする引数の値
	 * @param min 許容最小値
	 * @param max 許容最大値
	 * @param valueName 引数の名前
	 * @throws IllegalArgumentException 引数の値が許容範囲外だった
	 */
	public static void assertArgRange(float value, float min, float max, String valueName) {
		if ((value < min) || (value > max)) assertArgRangeFail(value, min, max, valueName);
	}

	/**
	 * 引数の値の範囲をテストするアサーション
	 * @param value テストする引数の値
	 * @param min 許容最小値
	 * @param max 許容最大値
	 * @param valueName 引数の名前
	 * @throws IllegalArgumentException 引数の値が許容範囲外だった
	 */
	public static void assertArgRange(double value, double min, double max, String valueName) {
		if ((value < min) || (value > max)) assertArgRangeFail(value, min, max, valueName);
	}

	/**
	 * IllegalArgumentExceptionをスローする
	 * @param value テストする引数の値
	 * @param min 許容最小値
	 * @param max 許容最大値
	 * @param valueName 引数の名前
	 */
	private static <T> void assertArgRangeFail(T value, T min, T max, String valueName) {
		var msg = String.format("Argument '%s' is out of range. expect=(%s-%s), actual=%s", valueName, min, max, value);
		throw new IllegalArgumentException(msg);
	}

	/**
	 * 引数のインデックス値の範囲をテストするアサーション
	 * @param index インデックス
	 * @param count 要素の最大数
	 * @param argName 引数の名前
	 * @throws IndexOutOfBoundsException インデックスが範囲外
	 */
	public static void assertArgIndexRange(int index, int count, String argName) {
		if ((index < 0) || (index >= count)) {
			var msg = String.format("Argument '%s' is out of range. 0 <= index < %d, but %d.", argName, count, index);
			throw new IndexOutOfBoundsException(msg);
		}
	}

	/**
	 * クラスフィールドの内容をテストする汎用アサーション
	 * @param success クラスフィールドをテストする評価式の結果
	 * @param format アサーション失敗時のメッセージ書式
	 * @param args メッセージの引数
	 * @throws IllegalStateException アサーションに失敗した
	 */
	public static void assertField(boolean success, String format, Object...args) {
		if (!success) {
			throw new IllegalStateException(String.format(format, args));
		}
	}
}
