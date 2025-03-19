package com.lmt.lib.bldt.parser;

import static com.lmt.lib.bldt.DifficultyTables.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;

import com.lmt.lib.bldt.ContentDescription;
import com.lmt.lib.bldt.Parser;
import com.lmt.lib.bldt.PlayStyle;
import com.lmt.lib.bldt.TableDescription;

/**
 * GENOCIDE の通常・発狂難易度表を掲載したWebページから楽曲情報を抽出するパーサです。
 *
 * <p>当パーサはSHIFT-JISでエンコードされたWebページ(HTML)から楽曲情報を抽出します。
 * 楽曲情報は JavaScript のソースコードとして定義されており、その中から楽曲情報定義部分の JSON を検出します。
 * 楽曲情報の定義は以下のようになっていることを前提として抽出します。</p>
 *
 * <pre>
 * var mname = [
 *   [
 *     (番号),
 *     "(難易度ラベル)",
 *     "(タイトル)",
 *     "(LR2IRのID)",
 *     "&lt;a href='(楽曲本体入手先URL)'&gt;(アーティスト)&lt;/a&gt;",
 *     "(楽曲コメント)"
 *   ], …以下複数
 * ];</pre>
 *
 * @since 0.1.0
 */
public class GenocideHtmlParser implements Parser {
	/** 楽曲情報定義の開始部分の正規表現パターン */
	private static final Pattern CONTENTS_BEGIN_PATTERN = Pattern.compile("^\\s*var\\s+mname\\s*=\\s*\\[\\s*$");
	/** 楽曲情報定義の終了部分の正規表現パターン */
	private static final Pattern CONTENTS_END_PATTERN = Pattern.compile("^\\s*\\];\\s*$");
	/** 楽曲本体入手先URL、およびアーティスト定義部分の正規表現パターン */
	private static final Pattern ARTIST_PATTERN = Pattern.compile("^<a\\s+href\\s*=\\s*'([^']*)'\\s*>?(.+)</a>$");

	/** {@inheritDoc} */
	@Override
	public List<ContentDescription> parse(TableDescription tableDesc, PlayStyle playStyle, byte[] raw)
			throws IOException {
		// 楽曲情報リストを抽出する
		var jsonContents = (JSONArray)null;
		try {
			// 生HTMLソースから楽曲情報部のJSONを抽出する
			// GENOCIDEの楽曲情報では、厳格なJSONの仕様として一部不正なエスケープ文字として認識される箇所があり、
			// そのままJSON解析にかけるとエラーになるので、不正エスケープ部分をスキップできるよう、その部分を
			// エスケープなしの文字列に変換した後でJSON解析を行うようにする。
			var end = new AtomicBoolean(false);
			var bais = new ByteArrayInputStream(raw);
			var isr = new InputStreamReader(bais, Charset.forName("MS932"));
			var reader = new BufferedReader(isr);
			var json = reader.lines()
					.dropWhile(l -> !CONTENTS_BEGIN_PATTERN.matcher(l).matches())
					.skip(1)
					.peek(l -> end.set(CONTENTS_END_PATTERN.matcher(l).matches()))
					.takeWhile(l -> !end.get())
					.collect(Collectors.joining("\n", "[", "]"))
					.replaceAll("\\\\([^\"/\\\\bfnrtu])", "\\\\\\\\$1");

			// 楽曲情報リストをJSON配列として解析する
			jsonContents = new JSONArray(json);
			if (jsonContents.isEmpty() || !end.get()) {
				// 楽曲情報リストの開始と終了を検知できなかった場合、0件の場合は解析エラーとする
				// ※楽曲情報が空＝難易度表が正しく作成されていない、若しくは構文エラーとして解釈する
				throw new IOException("Unknown or invalid HTML source");
			}
		} catch (JSONException e) {
			// JSON解析エラーの場合は処理を中止する
			throw new IOException("JSON parse error", e);
		}

		// 楽曲情報を解体して楽曲情報リストを生成する
		var styleDesc = tableDesc.getPlayStyleDescription(playStyle);
		var symbol = styleDesc.getSymbol();
		var count = jsonContents.length();
		var contents = new ArrayList<ContentDescription>(count);
		for (var i = 0; i < count; i++) {
			// 1件ずつ楽曲情報を抽出する
			var jsonContent = jsonContents.optJSONArray(i);
			if (Objects.isNull(jsonContent)) {
				// 配列データではない項目はスキップする
				printLog("contents[%d]: Skip because bad data format", i);
				continue;
			}
			if (jsonContent.length() < 5) {
				// 配列データの件数が必要要素数に満たない項目はスキップする
				printLog("contents[%d]: Skip because too few data definition: Length=%d", i, jsonContent.length());
				continue;
			}

			// 難易度表記から難易度インデックスを解決する
			// GENOCIDE HTMLでは楽曲情報に難易度表記(記号+難易度ラベル)を記述しているので、インデックス値に変換する
			var level = jsonContent.getString(1);
			if (!level.startsWith(symbol)) {
				// 難易度表記が記号で始まっていない場合は不正データとして扱う
				printLog("contents[%d]: Skip because invalid level: Value='%s'", level);
				continue;
			}
			var label = level.substring(symbol.length());
			var levelIndex = styleDesc.getLevelIndex(label);
			if (levelIndex < 0) {
				// 難易度表に該当するラベルが存在しない場合は不正データとして扱う
				printLog("contents[%d]: Skip because in this table, no level such '%s'", i, level);
				continue;
			}

			// タイトルを抽出する
			var title = jsonContent.optString(2, "");
			if (title.isEmpty()) {
				printLog("contents[%d]: Skip because title is empty", i);
				continue;
			}

			// アーティストと楽曲本体入手先URLを抽出する
			var artist = "";
			var bodyUrlStr = (String)null;
			var bodyUrl = (URL)null;
			var artistOrg = jsonContent.optString(4, "");
			var artistMatcher = ARTIST_PATTERN.matcher(artistOrg);
			if (artistMatcher.matches()) {
				// アーティストと楽曲本体入手先URLの両方を抽出可能
				artist = artistMatcher.group(2);
				bodyUrlStr = artistMatcher.group(1);
			} else {
				// アーティストのみ抽出可能
				printLog("contents[%d]: Abnormal artist pattern: Org=%s", i, artistOrg);
				artist = artistOrg;
			}
			if (Objects.nonNull(bodyUrlStr) && !bodyUrlStr.isEmpty()) {
				try {
					// 本体入手先URLを解析する
					bodyUrl = new URL(bodyUrlStr);
				} catch (MalformedURLException e) {
					printLog("contents[%d]: Invalid body URL: Value='%s'", i, bodyUrlStr);
				}
			}

			// 楽曲情報を構築する
			contents.add(new ContentDescription(title, artist, playStyle, levelIndex, bodyUrl, null, null, null));
		}

		return contents;
	}
}
