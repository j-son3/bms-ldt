package com.lmt.lib.bldt;

import static com.lmt.lib.bldt.internal.Assertion.*;

import java.net.URL;
import java.util.Objects;

import com.lmt.lib.bldt.internal.Utility;

/**
 * 難易度表の定義内容を表すクラスです。
 *
 * <p>当クラスでは1個の難易度表が保有する情報をまとめます。必要に応じて情報を参照し、アプリケーションを実装してください。
 * 参照可能な情報については当クラスの getter メソッドを参照してください。</p>
 *
 * @since 0.1.0
 */
public class TableDescription {
	/** ID */
	private String mId;
	/** 難易度表名称 */
	private String mName;
	/** 公式URL */
	private URL mOfficialUrl;
	/** 元データ解析用のパーサオブジェクト */
	private Parser mParser;
	/** プレースタイル定義 */
	private PlayStyleDescription[] mStyleDescs = new PlayStyleDescription[PlayStyle.COUNT];
	/** ライブラリ標準の難易度表かどうか */
	private boolean mIsPreset;

	/**
	 * 新しい難易度表定義オブジェクトを構築します。
	 * <p>IDは1文字以上の半角英数字とアンダースコアで、1文字目には数字は使用できません。
	 * この規約に則った形式の文字列を指定するようにしてください。</p>
	 * <p>難易度表名称には任意の文字を使用可能ですが、空文字列は指定できません。</p>
	 * <p>プレースタイルごとに異なる情報はシングルプレー、ダブルプレーに分けて定義します。
	 * 非サポートのプレースタイルには null を指定してください。ただし、両方を非サポートにすることはできません。</p>
	 * @param id ID
	 * @param name 難易度表名称
	 * @param officialUrl 公式URL
	 * @param parser 元データ解析用のパーサオブジェクト
	 * @param spDesc シングルプレーのプレースタイル定義
	 * @param dpDesc ダブルプレーのプレースタイル定義
	 * @throws NullPointerException ID, 難易度表名称, 公式URL, 元データ解析用のパーサオブジェクトのいずれかがnull
	 * @throws IllegalArgumentException IDの形式が不正
	 * @throws IllegalArgumentException 難易度表名称が空文字列
	 * @throws IllegalArgumentException プレースタイル定義が全てnull
	 * @since 0.1.0
	 */
	public TableDescription(String id, String name, URL officialUrl, Parser parser, PlayStyleDescription spDesc,
			PlayStyleDescription dpDesc) {
		assertArgNotNull(id, "id");
		assertArg(Utility.isIdValid(id), "'id' is not valid: %s", id);
		assertArgNotNull(name, "name");
		assertArg(!name.isEmpty(), "'name' is empty");
		assertArgNotNull(officialUrl, "officialUrl");
		assertArgNotNull(parser, "parser");
		assertArg(Objects.nonNull(spDesc) || Objects.nonNull(dpDesc), "All play styles are disabled");
		mId = id;
		mName = name;
		mOfficialUrl = officialUrl;
		mParser = parser;
		mStyleDescs[PlayStyle.SINGLE.ordinal()] = spDesc;
		mStyleDescs[PlayStyle.DOUBLE.ordinal()] = dpDesc;
		mIsPreset = false;
	}

	/**
	 * IDを取得します。
	 * @return ID
	 * @since 0.1.0
	 */
	public String getId() {
		return mId;
	}

	/**
	 * 難易度表名称を取得します。
	 * @return 難易度表名称
	 * @since 0.1.0
	 */
	public String getName() {
		return mName;
	}

	/**
	 * 難易度表名称設定
	 * @param name 難易度表名称
	 */
	void setName(String name) {
		mName = name;
	}

	/**
	 * 公式URLを取得します。
	 * @return 公式URL
	 * @since 0.1.0
	 */
	public URL getOfficialUrl() {
		return mOfficialUrl;
	}

	/**
	 * 元データ解析用のパーサオブジェクトを取得します。
	 * @return 元データ解析用のパーサオブジェクト
	 * @since 0.1.0
	 */
	public Parser getParser() {
		return mParser;
	}

	/**
	 * 指定したプレースタイル定義を取得します。
	 * @param playStyle プレースタイル
	 * @return プレースタイル定義
	 * @throws NullPointerException playStyle が null
	 * @since 0.1.0
	 */
	public PlayStyleDescription getPlayStyleDescription(PlayStyle playStyle) {
		assertArgNotNull(playStyle, "playStyle");
		return mStyleDescs[playStyle.ordinal()];
	}

	/**
	 * シングルプレーのプレースタイル定義を取得します。
	 * @return シングルプレーのプレースタイル定義、非サポート時は null
	 * @since 0.1.0
	 */
	public PlayStyleDescription getSingleDescription() {
		return mStyleDescs[PlayStyle.SINGLE.ordinal()];
	}

	/**
	 * ダブルプレーのプレースタイル定義を取得します。
	 * @return ダブルプレーのプレースタイル定義、非サポート時は null
	 * @since 0.1.0
	 */
	public PlayStyleDescription getDoubleDescription() {
		return mStyleDescs[PlayStyle.DOUBLE.ordinal()];
	}

	/**
	 * この難易度表定義がライブラリ標準の定義かどうかを取得します。
	 * @return ライブラリ標準の難易度表定義の場合 true
	 * @since 0.1.0
	 */
	public boolean isPreset() {
		return mIsPreset;
	}

	/**
	 * ライブラリ標準の難易度表定義かどうかを設定
	 * @param isPreset ライブラリ標準の難易度表定義かどうか
	 */
	void setIsPreset(boolean isPreset) {
		mIsPreset = isPreset;
	}
}
