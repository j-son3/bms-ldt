package com.lmt.lib.bldt;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.lmt.lib.bldt.parser.GenocideHtmlParser;
import com.lmt.lib.bldt.parser.ScoreJsonParser;

/**
 * LDTライブラリがサポートする難易度表を表す列挙型です。
 *
 * <p>当ライブラリはいくつかの難易度表を標準でサポートしています。当列挙型の値を参照することで、
 * サポート対象となっている難易度表の定義にアクセスすることができます。列挙値がそれぞれ保有する難易度表の定義は
 * {@link TableDescription#isPreset()} が true を返します。</p>
 *
 * @since 0.1.0
 */
public enum Presets {
	/**
	 * GENOCIDE 通常難易度表
	 * @since 0.1.0
	 */
	GENOCIDE_NORMAL(
			"genocide_n",
			"https://nekokan.dyndns.info/~lobsak/genocide/",
			new GenocideHtmlParser(),
			"☆",
			"https://nekokan.dyndns.info/~lobsak/genocide/normal.html",
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "X"),
			null,
			null,
			null),
	/**
	 * GENOCIDE 発狂難易度表
	 * @since 0.1.0
	 */
	GENOCIDE_INSANE(
			"genocide_i",
			"https://nekokan.dyndns.info/~lobsak/genocide/",
			new GenocideHtmlParser(),
			"★",
			"https://nekokan.dyndns.info/~lobsak/genocide/insane.html",
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
					"17", "18", "19", "20", "21", "22", "23", "24", "25", "???"),
			null,
			null,
			null),
	/**
	 * δ 通常難易度表
	 * @since 0.1.0
	 */
	DELTA_NORMAL(
			"delta_n",
			"https://deltabms.yaruki0.net/",
			new ScoreJsonParser(),
			null,
			null,
			null,
			"δ",
			"https://deltabms.yaruki0.net/table/data/dpdelta_data.json",
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "97", "98", "99")),
	/**
	 * δ 発狂難易度表
	 * @since 0.1.0
	 */
	DELTA_INSANE(
			"delta_i",
			"https://deltabms.yaruki0.net/",
			new ScoreJsonParser(),
			null,
			null,
			null,
			"★",
			"https://deltabms.yaruki0.net/table/data/insane_data.json",
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "?")),
	/**
	 * NEW GENERATION 通常難易度表
	 * @since 0.1.0
	 */
	NEW_GENERATION_NORMAL(
			"newgen_n",
			"https://rattoto10.jounin.jp/",
			new ScoreJsonParser(),
			"▽",
			"https://rattoto10.github.io/second_table/score.json",
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "11+", "12-", "12", "12+", "？"),
			null,
			null,
			null),
	/**
	 * NEW GENERATION 発狂難易度表
	 * @since 0.1.0
	 */
	NEW_GENERATION_INSANE(
			"newgen_i",
			"https://rattoto10.jounin.jp/",
			new ScoreJsonParser(),
			"▼",
			"https://rattoto10.github.io/second_table/insane_data.json",
			List.of("0-", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
					"15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "?"),
			null,
			null,
			null),
	/**
	 * Satellite難易度表
	 * @since 0.1.0
	 */
	SATELLITE(
			"satellite",
			"https://stellabms.xyz/",
			new ScoreJsonParser(),
			"sl",
			"https://stellabms.xyz/sl/score.json",
			List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"),
			"DPsl",
			"https://stellabms.xyz/dp/score.json",
			List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")),
	/**
	 * Stella難易度表
	 * @since 0.1.0
	 */
	STELLA(
			"stella",
			"https://stellabms.xyz/",
			new ScoreJsonParser(),
			"st",
			"https://stellabms.xyz/st/score.json",
			List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"),
			"DPst",
			"https://stellabms.xyz/dpst/score.json",
			List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")),
	/**
	 * Solar難易度表
	 * @since 0.2.0
	 */
	SOLAR(
			"solar",
			"https://stellabms.xyz/",
			new ScoreJsonParser(),
			"so",
			"https://stellabms.xyz/so/score.json",
			List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"),
			null,
			null,
			null),
	/**
	 * Supernova難易度表
	 * @since 0.2.0
	 */
	SUPERNOVA(
			"supernova",
			"https://stellabms.xyz/",
			new ScoreJsonParser(),
			"sn",
			"https://stellabms.xyz/sn/score.json",
			List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"),
			null,
			null,
			null),
	/**
	 * Overjoy難易度表
	 * @since 0.1.0
	 */
	OVERJOY(
			"overjoy",
			"https://rattoto10.jounin.jp/",
			new ScoreJsonParser(),
			"★★",
			"https://rattoto10.github.io/second_table/overjoy_score.json",
			List.of("0", "1", "2", "3", "4", "5", "6", "7", "8"),
			"★★",
			"http://ereter.net/static/analyzer/json/overjoy.json",
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "99")),
	/**
	 * LN難易度表
	 * @since 0.1.0
	 */
	LONG_NOTE(
			"long_note",
			"http://flowermaster.web.fc2.com/lrnanido/bmsln.html",
			new ScoreJsonParser(),
			"◆",
			"http://flowermaster.web.fc2.com/lrnanido/gla/score.json",
			List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
					"17", "18", "19", "20", "21", "22", "23", "24", "25", "26"),
			null,
			null,
			null),
	/**
	 * Scramble難易度表
	 * @since 0.1.0
	 */
	SCRAMBLE(
			"scramble",
			"https://egret9.github.io/Scramble/",
			new ScoreJsonParser(),
			"SB",
			"https://script.google.com/macros/s/AKfycbw5pnMwlCFZz7wDY5kRsBpfSm0-luKszs8LQAEE6BKkVT1R78-CpB4WA9chW-gdBsF7IA/exec",
			List.of("-1", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"),
			null,
			null,
			null),
	/**
	 * 癖譜面ライブラリー
	 * @since 0.1.1
	 */
	UNIQUE(
			"unique",
			"https://rattoto10.web.fc2.com/kuse_library/main.html",
			new ScoreJsonParser(),
			"癖",
			"https://rattoto10.web.fc2.com/kuse_library/score.json",
			List.of("0", "1" ,"2" ,"3" ,"4" ,"5" ,"6" ,"7" ,"8" ,"9" ,
					"10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
					"20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
					"30", "31", "51", "52", "53", "54", "55", "56", "99"),
			null,
			null,
			null);

	/** 難易度表定義 */
	private TableDescription mTableDesc;

	/**
	 * コンストラクタ
	 * @param id ID
	 * @param officialUrl 公式URL
	 * @param parser 元データ解析用のパーサ
	 * @param spSymbol シングルプレーの記号
	 * @param spContentUrl シングルプレーの楽曲情報URL
	 * @param spLabels シングルプレーの難易度ラベルリスト
	 * @param dpSymbol ダブルプレーの記号
	 * @param dpContentUrl ダブルプレーの楽曲情報URL
	 * @param dpLabels ダブルプレーの難易度ラベルリスト
	 */
	private Presets(String id, String officialUrl, Parser parser,
			String spSymbol, String spContentUrl, Collection<String> spLabels,
			String dpSymbol, String dpContentUrl, Collection<String> dpLabels) {
		try {
			var spDesc = (PlayStyleDescription)null;
			if (Objects.nonNull(spSymbol)) {
				spDesc = new PlayStyleDescription(spSymbol, new URL(spContentUrl), spLabels);
			}
			var dpDesc = (PlayStyleDescription)null;
			if (Objects.nonNull(dpSymbol)) {
				dpDesc = new PlayStyleDescription(dpSymbol, new URL(dpContentUrl), dpLabels);
			}
			mTableDesc = new TableDescription(id, id, new URL(officialUrl), parser, spDesc, dpDesc);
			mTableDesc.setIsPreset(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * この難易度表のIDを取得します。
	 * @return ID
	 * @since 0.1.0
	 */
	public String getId() {
		return mTableDesc.getId();
	}

	/**
	 * この難易度表の定義を取得します。
	 * @return 難易度表定義
	 * @since 0.1.0
	 */
	public TableDescription getTableDescription() {
		return mTableDesc;
	}
}
