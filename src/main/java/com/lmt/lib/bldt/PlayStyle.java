package com.lmt.lib.bldt;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * プレースタイルを表す列挙型です。
 *
 * @since 0.1.0
 */
public enum PlayStyle {
	/**
	 * シングルプレー
	 * @since 0.1.0
	 */
	SINGLE("sp"),
	/**
	 * ダブルプレー
	 * @since 0.1.0
	 */
	DOUBLE("dp");

	/** 全プレースタイルリスト */
	static final List<PlayStyle> ALL =  Arrays.stream(values()).collect(Collectors.toUnmodifiableList());
	/** 全プレースタイルの件数 */
	static final int COUNT = ALL.size();

	/** 短い名前 */
	final String shortName;

	/**
	 * コンストラクタ
	 * @param shortName 短い名前
	 */
	private PlayStyle(String shortName) {
		this.shortName = shortName;
	}

	/**
	 * ブール値からプレースタイル取得
	 * @param dpMode ダブルプレーかどうか
	 * @return プレースタイル
	 */
	static PlayStyle fromBoolean(boolean dpMode) {
		return dpMode ? DOUBLE : SINGLE;
	}

	/**
	 * 序列からプレースタイル
	 * @param ordinal 序列
	 * @return プレースタイル
	 */
	static PlayStyle fromOrdinal(int ordinal) {
		return ALL.get(ordinal);
	}
}
