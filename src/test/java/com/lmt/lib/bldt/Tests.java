package com.lmt.lib.bldt;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.function.Function;

/**
 * テストで使用することを想定した機能の集合です。
 * @author J-SON3
 */
public class Tests {
	/**
	 * テスト用の出力ファイルを格納するための一時ディレクトリを作成します。
	 * <p>通常、このディレクトリはテストクラスでのテスト開始時に当メソッドを呼び出して作成し、テスト終了後に
	 * {@link #rmtmpdir(Class)}を呼び出してディレクトリごと破棄するような用途で使用します。
	 * テスト結果を証明するためのエビデンスとして出力ファイルを残す場合は前述の破棄処理を実行しないことで実現できます。
	 * ただし、その際は当該テストクラスのテスト開始時に一時ディレクトリの中身を全て削除することを推奨します。</p>
	 * <p>当メソッドはカレントディレクトリの配下に、指定クラスの名称を利用してディレクトリを作成します。
	 * 同じ名前のディレクトリが既に存在する場合は何も行いません。</p>
	 * @param testClass テストクラス
	 * @return 作成されたディレクトリのパス
	 * @throws IOException ディレクトリ作成に失敗した
	 */
	public static Path mktmpdir(Class<?> testClass) throws IOException {
		Path dirPath = Paths.get(System.getProperty("user.dir"), String.format(".%s", testClass.getSimpleName()));
		if (!Files.isDirectory(dirPath)) {
			Files.createDirectory(dirPath);
		}
		return dirPath;
	}

	/**
	 * テスト用の出力ファイルを格納するための一時ディレクトリを削除します。
	 * <p>当メソッドはカレントディレクトリ配下にある一時ディレクトリを、ディレクトリ内にある全てのファイルごと削除します。
	 * 一時ディレクトリが存在しない場合は何も行いません。</p>
	 * @param testClass テストクラス
	 * @throws IOException ディレクトリ削除に失敗した
	 */
	public static void rmtmpdir(Class<?> testClass) throws IOException {
		Path dirPath = Paths.get(System.getProperty("user.dir"), String.format(".%s", testClass.getSimpleName()));
		if (Files.isDirectory(dirPath)) {
			Files.walk(dirPath)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}

	/**
	 * インスタンスフィールドの値を取得します。
	 * @param <T> クラスフィールドの型
	 * @param o オブジェクトのインスタンス
	 * @param name フィールド名
	 * @return nameに該当するインスタンスフィールドの値
	 * @throws Exception 値の取得に失敗した時
	 */
	public static <T> T getf(Object o, String name) throws Exception {
		return getField(o.getClass(), o, name);
	}

	/**
	 * クラスフィールドの値を取得します。
	 * @param <T> クラスフィールドの型
	 * @param c 取得対象のクラスフィールドを保有するクラス
	 * @param name フィールド名
	 * @return nameに該当するクラスフィールドの値
	 * @throws Exception 値の取得に失敗した時
	 */
	public static <T> T getsf(Class<?> c, String name) throws Exception {
		return getField(c, null, name);
	}

	/**
	 * インスタンスフィールドの値を設定します。
	 * @param o オブジェクトのインスタンス
	 * @param name フィールド名
	 * @param value 設定する値
	 * @throws Exception 値の設定に失敗した時
	 */
	public static void setf(Object o, String name, Object value) throws Exception {
		setField(o.getClass(), o, name, value);
	}

	/**
	 * クラスフィールドの値を設定します。
	 * @param c 設定対象のクラスフィールドを保有するクラス
	 * @param name フィールド名
	 * @param value 設定する値
	 * @throws Exception 値の設定に失敗した時
	 */
	public static void setsf(Class<?> c, String name, Object value) throws Exception {
		setField(c, null, name, value);
	}

	/**
	 * 指定されたクラスの新しいインスタンスを生成します。
	 * @param <C> インスタンス生成対象クラスの型
	 * @param c インスタンス生成対象クラス
	 * @param params コンストラクタのパラメータ一覧
	 * @return 生成された新しいインスタンス
	 * @throws Exception インスタンスの生成に失敗した時
	 */
	public static <C> C newobj(Class<?> c, Object...params) throws Exception {
		return callMethod(c, Class::getDeclaredConstructors, null, c.getName(), params);
	}

	/**
	 * インスタンスメソッドを実行します。
	 * @param <R> 戻り値の型
	 * @param o オブジェクトのインスタンス
	 * @param method メソッド名
	 * @param params メソッドのパラメータ一覧
	 * @return 実行したメソッドの戻り値
	 * @throws Exception メソッドの実行に失敗した時
	 */
	public static <R> R call(Object o, String method, Object...params) throws Exception {
		return callMethod(o.getClass(), Class::getDeclaredMethods, o, method, params);
	}

	/**
	 * クラスメソッドを実行します。
	 * @param <R> 戻り値の型
	 * @param c クラスメソッドを保有するクラス
	 * @param method メソッド名
	 * @param params メソッドのパラメータ一覧
	 * @return 実行したメソッドの戻り値
	 * @throws Exception メソッドの実行に失敗した時
	 */
	public static <R> R calls(Class<?> c, String method, Object...params) throws Exception {
		return callMethod(c, Class::getDeclaredMethods, null, method, params);
	}

	/**
	 * フィールドの値取得
	 * @param <T> フィールドの型
	 * @param c クラス
	 * @param o オブジェクトのインスタンス
	 * @param name フィールド名
	 * @return フィールドの値
	 * @throws Exception 値の取得に失敗した時
	 */
	private static <T> T getField(Class<?> c, Object o, String name) throws Exception {
		Field field = getDeclaredField(c, name);
		@SuppressWarnings("deprecation")
		boolean accessible = field.isAccessible();
		try {
			// アクセス権をONにする
			field.setAccessible(true);
		} catch (Exception e) {
			// 当該フィールドのアクセス権をONにできなかった
			fail(String.format("'%s#%s' Could not set accessible status.", c.getName(), name));
			return null;
		}
		try {
			// 指定オブジェクトからフィールドの値を取得する
			@SuppressWarnings("unchecked")
			T value = (T)field.get(o);
			return value;
		} finally {
			try {
				// アクセス権を元に戻す
				field.setAccessible(accessible);
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

	/**
	 * フィールドの値設定
	 * @param c クラス
	 * @param o オブジェクトのインスタンス
	 * @param name フィールド名
	 * @param value 設定する値
	 * @throws Exception 値の設定に失敗した時
	 */
	private static void setField(Class<?> c, Object o, String name, Object value) throws Exception {
		Field field = getDeclaredField(c, name);
		@SuppressWarnings("deprecation")
		boolean accessible = field.isAccessible();
		try {
			// アクセス権をONにする
			field.setAccessible(true);
		} catch (Exception e) {
			// 当該フィールドのアクセス権をONにできなかった
			fail(String.format("'%s#%s' Could not set accessible status.", c.getName(), name));
			return;
		}
		try {
			// 指定オブジェクトへフィールドの値を設定する
			field.set(o, value);
		} finally {
			try {
				// アクセス権を元に戻す
				field.setAccessible(accessible);
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

	/**
	 * メソッド実行
	 * @param <R> 戻り値の型
	 * @param c クラス
	 * @param getExecs 関数一覧取得用の関数
	 * @param o オブジェクトのインスタンス
	 * @param name メソッド名
	 * @param params パラメータ一覧
	 * @return メソッドの戻り値
	 * @throws Exception メソッドの実行に失敗した時
	 */
	@SuppressWarnings("unchecked")
	private static <R> R callMethod(Class<?> c, Function<Class<?>, Executable[]> getExecs, Object o, String name,
			Object... params) throws Exception {
		// 該当する関数を検索する
		Executable targetFunc = getDeclaredExecutable(c, getExecs, name, params);

		// 関数のアクセス権を一時的に変更する
		@SuppressWarnings("deprecation")
		boolean accessible = targetFunc.isAccessible();
		try {
			// アクセス権をONにする
			targetFunc.setAccessible(true);
		} catch (Exception e) {
			// 当該関数のアクセス権をONにできなかった
			fail(String.format("'%s#%s' Could not set accessible status.", c.getName(), name));
			return null;
		}

		// 関数を実行する
		try {
			if (targetFunc instanceof Method) {
				// メソッド
				return (R)((Method)targetFunc).invoke(o, params);
			} else if (targetFunc instanceof Constructor) {
				// コンストラクタ
				return (R)((Constructor<R>)targetFunc).newInstance(params);
			} else {
				// 不明な種類の関数
				fail(String.format("'%s' Unknown function type. (Class=%s)", name, c.getName()));
				return null;
			}
		} finally {
			try {
				// アクセス権を元に戻す
				targetFunc.setAccessible(accessible);
			} catch (Exception e) {
				// Do nothing
			}
		}
	}

	/**
	 * 指定された名前のフィールド取得
	 * @param c クラス
	 * @param name フィールド名
	 * @return nameに該当するフィールド
	 */
	private static Field getDeclaredField(Class<?> c, String name) {
		if (c == null) {
			fail("Class is not specified.");
			return null;
		} else if (name == null) {
			fail("Field name is not specified.");
			return null;
		}

		// 親クラスにまで遡ってフィールドを検索する
		Field field = null;
		while (c != null) {
			try {
				// クラスから指定された名前に該当するフィールドを取得する
				// 取得できた時点でそのフィールドを返す
				field = c.getDeclaredField(name);
				break;
			} catch (NoSuchFieldException e) {
				// 該当フィールドがない場合は親クラスに遡ってフィールド取得を試みる
				c = c.getSuperclass();
			}
		}

		// 存在しない場合はエラー
		if (field == null) {
			fail(String.format("'%s' No such field in '%s'", name, c.getName()));
			return null;
		}

		return field;
	}

	/**
	 * 指定された名前、パラメータ一覧と互換性のある関数取得
	 * @param c クラス
	 * @param getExecs 関数一覧取得用関数
	 * @param name 関数名
	 * @param params パラメータ一覧
	 * @return name, paramsと互換性のある関数
	 */
	private static Executable getDeclaredExecutable(Class<?> c, Function<Class<?>, Executable[]> getExecs,
			String name, Object... params) {
		if (c == null) {
			fail("Class is not specified.");
			return null;
		} else if (name == null) {
			fail("Method name is not specified.");
			return null;
		}

		// 親クラスにまで遡ってメソッドを検索する
		Executable targetFunc = null;
		boolean foundSameName = false;
		while ((c != null) && (targetFunc == null)) {
			// マッチするメソッドを検索する
			Executable[] execs = getExecs.apply(c);
			for (int i = 0; (i < execs.length) && (targetFunc == null); i++) {
				boolean match = isCompatibleFunction(execs[i], name, params);
				targetFunc = match ? execs[i] : null;
				foundSameName = foundSameName || name.equals(execs[i].getName());
			}

			// 親クラスを遡って検索する
			c = c.getSuperclass();
		}

		// 該当する関数が存在しない場合はエラー
		if (targetFunc == null) {
			String msg;
			if (foundSameName) {
				// 同じ名前の関数が見つかったが、引数に互換性がない
				msg = String.format("'%s' Found function but incompatible parameters.", name);
			} else {
				// 同じ名前の関数が見つからなかった
				msg = String.format("'%s' No such function.", name);
			}
			fail(msg);
			return null;
		}

		return targetFunc;
	}

	/**
	 * 関数の互換性判定
	 * @param exe 関数
	 * @param name 関数名
	 * @param params パラメータ一覧
	 * @return 互換性がある場合true、なければfalse
	 */
	private static boolean isCompatibleFunction(Executable exe, String name, Object...params) {
		// 名前の一致しない関数は対象外
		if (!exe.getName().equals(name)) {
			return false;
		}

		// パラメータ数の一致しない関数は対象外
		int paramCount = exe.getParameterCount();
		if (paramCount != params.length) {
			return false;
		}

		// パラメータ数0で実際のパラメータ数値一致する場合は当該関数を対象とする
		if ((paramCount == 0) && (params.length == 0)) {
			return true;
		}

		// パラメータの型が一致しない関数は対象外
		Parameter[] funcParams = exe.getParameters();
		for (int i = 0; i < funcParams.length; i++) {
			Class<?> funcParamType = funcParams[i].getType();
			if (params[i] == null) {
				// 指定パラメータがnullの場合厳密な型チェックはできないので可能な限りのチェックを行う
				if (funcParamType.isPrimitive()) {
					return false;  // プリミティブ型パラメータにnullは指定できないので対象外
				}
			} else if (!funcParamType.isAssignableFrom(params[i].getClass())) {
				// 指定パラメータが代入不可能な場合のチェック
				if (!funcParamType.isPrimitive()) {
					return false;  // 引数がプリミティブ型でないならパラメータ非互換なので対象外
				}
				// TODO プリミティブ型へのオートアンボクシング可否をチェックする
			} else {
				// 指定パラメータが代入可能なので互換性ありと判定する
				// Do nothing
			}
		}

		// 名前とパラメータ数・型の一致した関数を対象とする
		return true;
	}
}
