package com.lmt.lib.bldt.parser;

import static com.lmt.lib.bldt.DifficultyTables.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;

import com.lmt.lib.bldt.ContentDescription;
import com.lmt.lib.bldt.Parser;
import com.lmt.lib.bldt.PlayStyle;
import com.lmt.lib.bldt.TableDescription;
import com.lmt.lib.bldt.internal.Utility;

/**
 * 楽曲情報 JSON から楽曲情報を抽出するパーサです。
 *
 * <p>楽曲情報 JSON は、多くの難易度表が採用している共通フォーマットの JSON です。
 * 実際には必須情報と数多くの任意情報で定義された JSON となっており、当ライブラリでは統一フォーマットとして使用可能な
 * 部分のみを抽出して難易度表全体の統一フォーマットとして昇華しています。</p>
 *
 * <p>具体的には以下のようなフォーマットの JSON を解析します。文字コードは UTF-8 を前提とします。</p>
 *
 * <pre>
 * [
 *   {
 *     &quot;level&quot;: &quot;(難易度を表す文字列)&quot;,
 *     &quot;title&quot;: &quot;(タイトル)&quot;,
 *     &quot;artist&quot;: &quot;(アーティスト)&quot;,
 *     &quot;url&quot;: &quot;(楽曲本体入手先URL)&quot;,
 *     &quot;url_diff&quot;: &quot;(差分譜面入手先URL)&quot;,
 *     &quot;md5&quot;: &quot;(ハッシュ値(MD5))&quot;,
 *     &quot;sha256&quot;: &quot;(ハッシュ値(SHA-256))&quot;,
 *     他、独自情報
 *   }, …以下複数
 * ]</pre>
 *
 * @since 0.1.0
 */
public class ScoreJsonParser implements Parser {
	@Override
	public List<ContentDescription> parse(TableDescription tableDesc, PlayStyle playStyle, byte[] raw)
			throws IOException {
		// 楽曲情報JSONを解析する
		// 入力されるJSONはUTF-8でエンコードされていることを前提とする
		var jsonContents = (JSONArray)null;
		try {
			var jsonStr = new String(raw, StandardCharsets.UTF_8);
			jsonContents = new JSONArray(jsonStr);
		} catch (JSONException e) {
			// 楽曲情報リストの開始と終了を検知できなかった場合は解析エラーとする
			throw new IOException("Bad JSON format", e);
		}

		// 楽曲情報を1件ずつ抽出する
		var styleDesc = tableDesc.getPlayStyleDescription(playStyle);
		var numContent = jsonContents.length();
		var outContents = new ArrayList<ContentDescription>(numContent);
		for (var i = 0; i < numContent; i++) {
			// 1件の楽曲情報を抽出する
			var jsonContent = jsonContents.optJSONObject(i);
			if (Objects.isNull(jsonContent)) {
				// 楽曲情報が想定外の定義の場合はスキップする
				printLog("contents[%d]: Bad data format", i);
				continue;
			}

			// 必須項目を抽出する
			var label = jsonContent.optString("level");
			var levelIndex = -1;
			var title = jsonContent.optString("title");
			var artist = jsonContent.optString("artist");
			if (Objects.isNull(label) || label.isEmpty()) {
				// レベル部が不正な場合はスキップする
				printLog("contents[%d]: Skip because invalid level: Value='%s'", i, label);
				continue;
			}
			if ((levelIndex = styleDesc.getLevelIndex(label)) < 0) {
				// 難易度表定義に存在しないレベルを検出した場合はスキップする
				printLog("contents[%d]: Skip because unknown level: Value='%s'", i, label);
				continue;
			}
			if (Objects.isNull(title) || title.isEmpty()) {
				// タイトルが不正な場合はスキップする
				printLog("contents[%d]: Skip because invalid title: Value='%s'", i, title);
				continue;
			}
			if (Objects.isNull(artist) || (artist.isEmpty() && !jsonContent.has("artist"))) {
				// アーティストが不正な場合はスキップする ※空文字はOKとする
				printLog("contents[%d]: Skip because invalid artist: Value='%s'", i, artist);
				continue;
			}

			// 任意項目を抽出する
			var fi = i;
			var bodyUrl = Utility.optionalJsonUrl(jsonContent.optString("url"), v -> {
				printLog("contents[%d]: Invalid body URL: Value='%s'", fi, v);
			});
			var addUrl = Utility.optionalJsonUrl(jsonContent.optString("url_diff"), v -> {
				printLog("contents[%d]: Invalid additional URL: Value='%s'", fi, v);
			});
			var md5 = Utility.optionalJsonHash(jsonContent.optString("md5"), Utility::isMd5, v -> {
				printLog("contents[%d]: Invalid MD5: Value='%s'", fi, v);
			});
			var sha256 = Utility.optionalJsonHash(jsonContent.optString("sha256"), Utility::isSha256, v -> {
				printLog("contents[%d]: Invalid SHA-256: Value='%s'", fi, v);
			});

			// 楽曲情報を追加する
			outContents.add(new ContentDescription(title, artist, playStyle, levelIndex, bodyUrl, addUrl, md5, sha256));
		}

		return outContents;
	}
}
