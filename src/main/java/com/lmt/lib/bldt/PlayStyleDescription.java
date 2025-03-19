package com.lmt.lib.bldt;

import static com.lmt.lib.bldt.internal.Assertion.*;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * プレースタイルごとの定義内容を表すクラスです。
 *
 * <p>難易度表はプレースタイルごとにサポート有無があり、また、データ取得元が異なります。
 * その他、各種情報はプレースタイルごとに差異があるため、それらの情報をまとめて当クラスで管理します。
 * 当クラスの情報は {@link TableDescription} の一部であり、同クラスから情報を取得することができます。</p>
 *
 * @since 0.1.0
 */
public class PlayStyleDescription {
	/** 記号 */
	private String mSymbol;
	/** 楽曲情報URL */
	private URL mContentUrl;
	/** 難易度ラベルリスト */
	private List<String> mLabels;
	/** 難易度ラベルによる難易度インデックスのマップ */
	private Map<String, Integer> mLevelIndices;

	/**
	 * 新しいプレースタイル定義オブジェクトを構築します。
	 * @param symbol 記号
	 * @param contentUrl 楽曲情報URL
	 * @param labels 難易度ラベルリスト
	 * @throws NullPointerException symbol, contentUrl, labels のいずれかが null
	 * @throws IllegalArgumentException symbol が空文字列
	 * @throws IllegalArgumentException labels が0件
	 * @throws IllegalArgumentException labels の中に空文字列が含まれている
	 * @throws IllegalArgumentException labels の中に同じ文字列が含まれている
	 * @since 0.1.0
	 */
	public PlayStyleDescription(String symbol, URL contentUrl, Collection<String> labels) {
		assertArgNotNull(symbol, "symbol");
		assertArg(!symbol.isEmpty(), "'symbol' is empty");
		assertArgNotNull(contentUrl, "contentUrl");
		assertArgNotNull(labels, "labels");
		assertArg(!labels.isEmpty(), "'labels' is empty");
		mSymbol = symbol;
		mContentUrl = contentUrl;
		mLabels = List.copyOf(labels);
		mLevelIndices = new HashMap<>();
		for (var i = 0; i < mLabels.size(); i++) {
			var label = mLabels.get(i);
			assertArg(!label.isEmpty(), "Can't specify empty string to label");
			assertArg(!mLevelIndices.containsKey(label), "Label '%s' is conflict", label);
			mLevelIndices.put(label, i);
		}
	}

	/**
	 * 記号を取得します。
	 * <p>記号とは、難易度表記の先頭部分に付加される文字列のことです。</p>
	 * @return 記号
	 * @since 0.1.0
	 */
	public String getSymbol() {
		return mSymbol;
	}

	/**
	 * 楽曲情報URLを取得します。
	 * <p>このURLから楽曲情報の元データがダウンロードされます。</p>
	 * @return 楽曲情報URL
	 * @since 0.1.0
	 */
	public URL getContentUrl() {
		return mContentUrl;
	}

	/**
	 * 難易度ラベルリストを取得します。
	 * <p>難易度ラベルとは、難易度表記のうち難易度の序列を表す文字列のことです。<br>
	 * 例：「★10」の数字 &quot;10&quot; の部分</p>
	 * <p>返されるリストは読み取り専用です。</p>
	 * @return 難易度ラベルリスト
	 * @since 0.1.0
	 */
	public List<String> getLabels() {
		return mLabels;
	}

	/**
	 * 指定した難易度ラベルに該当する難易度インデックスを取得します。
	 * @param label 難易度ラベル
	 * @return 難易度インデックス。存在しない難易度ラベルの場合 -1 。
	 * @throws NullPointerException label が null
	 * @since 0.1.0
	 */
	public int getLevelIndex(String label) {
		assertArgNotNull(label, "label");
		return mLevelIndices.getOrDefault(label, -1);
	}
}
