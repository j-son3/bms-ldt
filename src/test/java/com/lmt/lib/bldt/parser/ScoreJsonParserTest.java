package com.lmt.lib.bldt.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.lmt.lib.bldt.ContentDescription;
import com.lmt.lib.bldt.PlayStyle;
import com.lmt.lib.bldt.Presets;

public class ScoreJsonParserTest {
	// [JSON本体]
	// UTF-8でデコードされたJSONとして解析されること
	@Test
	public void testJson_Utf8() throws Exception {
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"あ\",\"artist\":\"い\"}",
				"]");
		assertEquals(1, l.size());
		assertEquals("あ", l.get(0).getTitle());
		assertEquals("い", l.get(0).getArtist());
	}

	// [JSON本体]
	// 空の配列の場合、楽曲情報リストが0件になること
	@Test
	public void testJson_EmptyArray() throws Exception {
		var l = parse("[]");
		assertTrue(l.isEmpty());
	}

	// [JSON本体]
	// object型の場合、IOExceptionがスローされること
	@Test
	public void testJson_ObjectType() throws Exception {
		assertThrows(IOException.class, () -> parse("{}"));
	}

	// [JSON本体]
	// JSON解析エラーの場合、IOExceptionがスローされること
	@Test
	public void testJson_ParseError() throws Exception {
		assertThrows(IOException.class, () -> parse("This is not JSON format"));
	}

	// [JSON本体]
	// 0バイトデータの場合、IOExceptionがスローされること
	@Test
	public void testJson_EmptyBytes() throws Exception {
		assertThrows(IOException.class, () -> parse(""));
	}

	// [楽曲情報]
	// 期待するフォーマットから正しく楽曲情報を抽出できること(正常系)
	@Test
	public void testContent_Normal() throws Exception {
		var md51 = "00000000ffffffff11111111eeeeeeee";
		var md52 = "00000000FFFFFFFF11111111EEEEEEEE";
		var sha2561 = "00000000ffffffff11111111eeeeeeee22222222dddddddd33333333cccccccc";
		var sha2562 = "00000000FFFFFFFF11111111EEEEEEEE22222222DDDDDDDD33333333CCCCCCCC";
		var l = parse(PlayStyle.SINGLE,
				"[",
				// 難易度ラベルのパターン
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"1\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"2\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"3\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"4\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"5\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"6\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"7\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"8\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"9\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"10\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"11\",\"title\":\"T\",\"artist\":\"A\"},",
				"  {\"level\":\"12\",\"title\":\"T\",\"artist\":\"A\"},",
				// タイトルのパターン
				"  {\"level\":\"0\",\"title\":\"TITLE\",\"artist\":\"A\"},",
				"  {\"level\":\"0\",\"title\":\" \",\"artist\":\"A\"},",
				"  {\"level\":\"0\",\"title\":\"日本語\",\"artist\":\"A\"},",
				"  {\"level\":\"0\",\"title\":\"한국어\",\"artist\":\"A\"},",
				// アーティストのパターン
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"ARTIST\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"日本語\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"한국어\"},",
				// 本体入手先URLのパターン
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\",\"url\":\"http://example.com/a.zip\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\",\"url\":\"https://example.com/a.zip\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\",\"url\":\"ftp://example.com/a.zip\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\",\"url\":\"http://example.com/%E3%81%82.zip\"},",
				// 差分譜面入手先URL
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\",\"url_diff\":\"http://example.com/a.zip\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\",\"url_diff\":\"https://example.com/a.zip\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\",\"url_diff\":\"ftp://example.com/a.zip\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\",\"url_diff\":\"http://example.com/%E3%81%82.zip\"},",
				// MD5のパターン
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"\",\"md5\":\"" + md51 + "\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"\",\"md5\":\"" + md52 + "\"},",
				// SHA-256のパターン
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"\",\"sha256\":\"" + sha2561 + "\"},",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"\",\"sha256\":\"" + sha2562 + "\"}",
				"]");
		var i = 0;
		assertEquals(0, l.get(i + 0).getLevelIndex());
		assertEquals(1, l.get(i + 1).getLevelIndex());
		assertEquals(2, l.get(i + 2).getLevelIndex());
		assertEquals(3, l.get(i + 3).getLevelIndex());
		assertEquals(4, l.get(i + 4).getLevelIndex());
		assertEquals(5, l.get(i + 5).getLevelIndex());
		assertEquals(6, l.get(i + 6).getLevelIndex());
		assertEquals(7, l.get(i + 7).getLevelIndex());
		assertEquals(8, l.get(i + 8).getLevelIndex());
		assertEquals(9, l.get(i + 9).getLevelIndex());
		assertEquals(10, l.get(i + 10).getLevelIndex());
		assertEquals(11, l.get(i + 11).getLevelIndex());
		assertEquals(12, l.get(i + 12).getLevelIndex());
		i += 13;
		assertEquals("TITLE", l.get(i + 0).getTitle());
		assertEquals(" ", l.get(i + 1).getTitle());
		assertEquals("日本語", l.get(i + 2).getTitle());
		assertEquals("한국어", l.get(i + 3).getTitle());
		i += 4;
		assertEquals("ARTIST", l.get(i + 0).getArtist());
		assertEquals("", l.get(i + 1).getArtist());
		assertEquals("日本語", l.get(i + 2).getArtist());
		assertEquals("한국어", l.get(i + 3).getArtist());
		i += 4;
		assertEquals("http://example.com/a.zip", l.get(i + 0).getBodyUrl().toString());
		assertEquals("https://example.com/a.zip", l.get(i + 1).getBodyUrl().toString());
		assertEquals("ftp://example.com/a.zip", l.get(i + 2).getBodyUrl().toString());
		assertEquals("http://example.com/%E3%81%82.zip", l.get(i + 3).getBodyUrl().toString());
		i += 4;
		assertEquals("http://example.com/a.zip", l.get(i + 0).getAdditionalUrl().toString());
		assertEquals("https://example.com/a.zip", l.get(i + 1).getAdditionalUrl().toString());
		assertEquals("ftp://example.com/a.zip", l.get(i + 2).getAdditionalUrl().toString());
		assertEquals("http://example.com/%E3%81%82.zip", l.get(i + 3).getAdditionalUrl().toString());
		i += 4;
		assertEquals(md51.toLowerCase(), l.get(i + 0).getMd5());
		assertEquals(md52.toLowerCase(), l.get(i + 1).getMd5());
		i += 2;
		assertEquals(sha2561.toLowerCase(), l.get(i + 0).getSha256());
		assertEquals(sha2562.toLowerCase(), l.get(i + 1).getSha256());
		l.forEach(cd -> assertEquals(PlayStyle.SINGLE, cd.getPlayStyle()));

		l = parse(PlayStyle.DOUBLE,
				"[",
				"  {\"level\":\"0\",\"title\":\"T\",\"artist\":\"A\"}",
				"]");
		assertEquals(PlayStyle.DOUBLE, l.get(0).getPlayStyle());
	}

	// [楽曲情報]
	// object型以外のデータは無視されること
	@Test
	public void testContent_NotObjectType() throws Exception {
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\"},",
				"  [],",
				"  \"string\",",
				"  {\"level\":\"1\",\"title\":\"A2\",\"artist\":\"B2\"},",
				"  99,",
				"  null",
				"]");
		assertEquals(2, l.size());
		assertEquals(0, l.get(0).getLevelIndex());
		assertEquals("A1", l.get(0).getTitle());
		assertEquals("B1", l.get(0).getArtist());
		assertEquals(1, l.get(1).getLevelIndex());
		assertEquals("A2", l.get(1).getTitle());
		assertEquals("B2", l.get(1).getArtist());
	}

	// [楽曲情報]
	// "level"が未定義、空文字列の場合は無視されること
	@Test
	public void testContent_LevelUndefineEmpty() throws Exception {
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\"},",
				"  {\"title\":\"A2\",\"artist\":\"B2\"}",
				"]");
		assertEquals(1, l.size());
		assertEquals("A1", l.get(0).getTitle());
	}

	// [楽曲情報]
	// "level"が難易度ラベルリストに存在しない場合は無視されること
	@Test
	public void testContent_LevelUnknown() throws Exception {
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\"},",
				"  {\"level\":\"UNKNOWN\",\"title\":\"A2\",\"artist\":\"B2\"}",
				"]");
		assertEquals(1, l.size());
		assertEquals("A1", l.get(0).getTitle());
	}

	// [楽曲情報]
	// "title"が未定義、空文字列の場合は無視されること
	@Test
	public void testContent_TitleUndefineEmpty() throws Exception {
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\"},",
				"  {\"level\":\"1\",\"artist\":\"B2\"},",
				"  {\"level\":\"2\",\"title\":\"\",\"artist\":\"B2\"}",
				"]");
		assertEquals(1, l.size());
		assertEquals("A1", l.get(0).getTitle());
	}

	// [楽曲情報]
	// "artist"が未定義の場合は無視されること
	@Test
	public void testContent_ArtistUndefine() throws Exception {
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\"},",
				"  {\"level\":\"1\",\"title\":\"A2\"}",
				"]");
		assertEquals(1, l.size());
		assertEquals("A1", l.get(0).getTitle());
	}

	// [楽曲情報]
	// "url"が未定義、不正URLの場合は本体入手先URLがnullになること
	@Test
	public void testContent_UrlUndefineInvalid() throws Exception {
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\",\"url\":\"http://example.com/a.zip\"},",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\"},",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\",\"url\":\"???\"}",
				"]");
		assertEquals(3, l.size());
		assertEquals("http://example.com/a.zip", l.get(0).getBodyUrl().toString());
		assertNull(l.get(1).getBodyUrl());
		assertNull(l.get(2).getBodyUrl());
	}

	// [楽曲情報]
	// "url_diff"が未定義、不正URLの場合は差分譜面入手先URLがnullになること
	@Test
	public void testContent_UrlDiffUndefineInvalid() throws Exception {
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\",\"url_diff\":\"http://example.com/a.zip\"},",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\"},",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\",\"url_diff\":\"???\"}",
				"]");
		assertEquals(3, l.size());
		assertEquals("http://example.com/a.zip", l.get(0).getAdditionalUrl().toString());
		assertNull(l.get(1).getAdditionalUrl());
		assertNull(l.get(2).getAdditionalUrl());
	}

	// [楽曲情報]
	// "md5"が未定義、不正ハッシュ値の場合はMD5がnullになること
	@Test
	public void testContent_Md5UndefineInvalid() throws Exception {
		var md5 = "00000000ffffffff11111111eeeeeeee";
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\",\"md5\":\"" + md5 + "\"},",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\"},",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\",\"md5\":\"???\"}",
				"]");
		assertEquals(3, l.size());
		assertEquals(md5, l.get(0).getMd5());
		assertNull(l.get(1).getMd5());
		assertNull(l.get(2).getMd5());
	}

	// [楽曲情報]
	// "sha256"が未定義、不正ハッシュ値の場合はSHA-256がnullになること
	@Test
	public void testContent_Sha256UndefineInvalid() throws Exception {
		var sha256 = "00000000ffffffff11111111eeeeeeee22222222dddddddd33333333cccccccc";
		var l = parse(
				"[",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\",\"sha256\":\"" + sha256 + "\"},",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\"},",
				"  {\"level\":\"0\",\"title\":\"A1\",\"artist\":\"B1\",\"sha256\":\"???\"}",
				"]");
		assertEquals(3, l.size());
		assertEquals(sha256, l.get(0).getSha256());
		assertNull(l.get(1).getSha256());
		assertNull(l.get(2).getSha256());
	}

	private static List<ContentDescription> parse(PlayStyle ps, String...src) throws Exception {
		return new ScoreJsonParser().parse(
				Presets.SATELLITE.getTableDescription(),
				ps,
				Stream.of(src).collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8));
	}

	private static List<ContentDescription> parse(String...src) throws Exception {
		return parse(PlayStyle.SINGLE, src);
	}
}
