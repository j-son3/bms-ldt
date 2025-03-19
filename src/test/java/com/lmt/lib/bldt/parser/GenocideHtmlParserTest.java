package com.lmt.lib.bldt.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.lmt.lib.bldt.ContentDescription;
import com.lmt.lib.bldt.PlayStyle;
import com.lmt.lib.bldt.Presets;

public class GenocideHtmlParserTest {
	// [HTML全体の構成]
	// var mname = [ から ]; までが解析対象になること
	@Test
	public void testHtml_Normal() throws Exception {
		var l = parse(
				"<html>",
				"<head></head>",
				"<body>",
				"<script language=\"javascript\" type=\"text/javascript\"><!--//",
				"var mname = [",
				"[1,\"★1\",\"TITLE\",\"0\",\"<a href='http://example.com/body.zip'>ARTIST</a>\"]",
				"];",
				"</script>",
				"</body>",
				"</html>");
		assertEquals(1, l.size());
		assertEquals("TITLE", l.get(0).getTitle());
		assertEquals("ARTIST", l.get(0).getArtist());
		assertEquals(0, l.get(0).getLevelIndex());
		assertEquals("http://example.com/body.zip", l.get(0).getBodyUrl().toString());
		assertNull(l.get(0).getAdditionalUrl());
		assertNull(l.get(0).getMd5());
		assertNull(l.get(0).getSha256());
	}

	// [HTML全体の構成]
	// 最初の mname が解析対象になること
	@Test
	public void testHtml_FirstMname() throws Exception {
		var l = parse(
				"var mname = [",
				"[1,\"★1\",\"TITLE1\",\"0\",\"ARTIST1\"]",
				"];",
				"var mname = [",
				"[2,\"★2\",\"TITLE2\",\"0\",\"ARTIST2\"]",
				"];");
		assertEquals(1, l.size());
		assertEquals("TITLE1", l.get(0).getTitle());
		assertEquals("ARTIST1", l.get(0).getArtist());
		assertEquals(0, l.get(0).getLevelIndex());
		assertNull(l.get(0).getBodyUrl());
		assertNull(l.get(0).getAdditionalUrl());
		assertNull(l.get(0).getMd5());
		assertNull(l.get(0).getSha256());
	}

	// [HTML全体の構成]
	// SHIFT-JISでデコードされて解析されること
	@Test
	public void testHtml_ShiftJis() throws Exception {
		var l = parse(
				"var mname = [",
				"[1,\"★1\",\"日本語のタイトル\",\"0\",\"日本語のアーティスト\"]",
				"];");
		assertEquals("日本語のタイトル", l.get(0).getTitle());
		assertEquals("日本語のアーティスト", l.get(0).getArtist());
	}

	// [HTML全体の構成]
	// 0バイトのデータを入力するとIOExceptionがスローされること
	@Test
	public void testHtml_EmptyRaw() throws Exception {
		assertThrows(IOException.class, () -> parse());
	}

	// [HTML全体の構成]
	// 楽曲情報定義の先頭が見つからないとIOExceptionがスローされること
	@Test
	public void testHtml_NonBeginContent() throws Exception {
		assertThrows(IOException.class, () -> {
			parse(
					"[1,\"★1\",\"TITLE\",\"0\",\"ARTIST\"]",
					"];");
		});
	}

	// [HTML全体の構成]
	// 楽曲情報定義の末尾が見つからないとIOExceptionがスローされること
	@Test
	public void testHtml_NonEndContent() throws Exception {
		assertThrows(IOException.class, () -> {
			parse(
					"var mname = [",
					"[1,\"★1\",\"TITLE\",\"0\",\"ARTIST\"]");
		});
	}

	// [楽曲情報定義]
	// 期待するフォーマットから正しく楽曲情報を抽出できること(正常系)
	@Test
	public void testContent_Normal() throws Exception {
		var l = parse(
				"var mname = [",
				// タイトルのパターン
				"  [1,\"★1\",\"TITLE\",\"0\",\"ARTIST\"],",
				"  [1,\"★1\",\" \",\"0\",\"ARTIST\"],",
				"  [1,\"★1\",\"日本語\",\"0\",\"ARTIST\"],",
				"  [1,\"★1\",\"\\ud55c\\uad6d\\uc5b4\",\"0\",\"ARTIST\"],",
				// アーティストのパターン
				"  [1,\"★1\",\"A\",\"0\",\"ARTIST\"],",
				"  [1,\"★1\",\"A\",\"0\",\"\"],",
				"  [1,\"★1\",\"A\",\"0\",\"日本語\"],",
				"  [1,\"★1\",\"A\",\"0\",\"\\ud55c\\uad6d\\uc5b4\"],",
				// 難易度表記のパターン
				"  [1,\"★1\",\"A\",\"0\",\"B\"],",
				"  [1,\"★2\",\"A\",\"0\",\"B\"],",
				"  [1,\"★3\",\"A\",\"0\",\"B\"],",
				"  [1,\"★4\",\"A\",\"0\",\"B\"],",
				"  [1,\"★5\",\"A\",\"0\",\"B\"],",
				"  [1,\"★6\",\"A\",\"0\",\"B\"],",
				"  [1,\"★7\",\"A\",\"0\",\"B\"],",
				"  [1,\"★8\",\"A\",\"0\",\"B\"],",
				"  [1,\"★9\",\"A\",\"0\",\"B\"],",
				"  [1,\"★10\",\"A\",\"0\",\"B\"],",
				"  [1,\"★11\",\"A\",\"0\",\"B\"],",
				"  [1,\"★12\",\"A\",\"0\",\"B\"],",
				"  [1,\"★13\",\"A\",\"0\",\"B\"],",
				"  [1,\"★14\",\"A\",\"0\",\"B\"],",
				"  [1,\"★15\",\"A\",\"0\",\"B\"],",
				"  [1,\"★16\",\"A\",\"0\",\"B\"],",
				"  [1,\"★17\",\"A\",\"0\",\"B\"],",
				"  [1,\"★18\",\"A\",\"0\",\"B\"],",
				"  [1,\"★19\",\"A\",\"0\",\"B\"],",
				"  [1,\"★20\",\"A\",\"0\",\"B\"],",
				"  [1,\"★21\",\"A\",\"0\",\"B\"],",
				"  [1,\"★22\",\"A\",\"0\",\"B\"],",
				"  [1,\"★23\",\"A\",\"0\",\"B\"],",
				"  [1,\"★24\",\"A\",\"0\",\"B\"],",
				"  [1,\"★25\",\"A\",\"0\",\"B\"],",
				"  [1,\"★???\",\"A\",\"0\",\"B\"],",
				// 本体入手先URLのパターン
				"  [1,\"★1\",\"A\",\"0\",\"<a href='http://example.com/1.zip'>B</a>\"],",
				"  [1,\"★1\",\"A\",\"0\",\"<a href='https://example.com/1.zip'>B</a>\"],",
				"  [1,\"★1\",\"A\",\"0\",\"<a href='ftp://example.com/1.zip'>B</a>\"],",
				"  [1,\"★1\",\"A\",\"0\",\"<a href='http://example.com/%E3%81%82.zip'>B</a>\"]",
				"];");
		var i = 0;
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
		assertEquals(13, l.get(i + 13).getLevelIndex());
		assertEquals(14, l.get(i + 14).getLevelIndex());
		assertEquals(15, l.get(i + 15).getLevelIndex());
		assertEquals(16, l.get(i + 16).getLevelIndex());
		assertEquals(17, l.get(i + 17).getLevelIndex());
		assertEquals(18, l.get(i + 18).getLevelIndex());
		assertEquals(19, l.get(i + 19).getLevelIndex());
		assertEquals(20, l.get(i + 20).getLevelIndex());
		assertEquals(21, l.get(i + 21).getLevelIndex());
		assertEquals(22, l.get(i + 22).getLevelIndex());
		assertEquals(23, l.get(i + 23).getLevelIndex());
		assertEquals(24, l.get(i + 24).getLevelIndex());
		assertEquals(25, l.get(i + 25).getLevelIndex());
		i += 26;
		assertEquals("http://example.com/1.zip", l.get(i + 0).getBodyUrl().toString());
		assertEquals("https://example.com/1.zip", l.get(i + 1).getBodyUrl().toString());
		assertEquals("ftp://example.com/1.zip", l.get(i + 2).getBodyUrl().toString());
		assertEquals("http://example.com/%E3%81%82.zip", l.get(i + 3).getBodyUrl().toString());
	}

	// [楽曲情報定義]
	// 配列型ではない項目は無視されること
	@Test
	public void testContent_NotArrayType() throws Exception {
		var l = parse(
				"var mname = [",
				"  [1,\"★1\",\"TITLE1\",\"0\",\"ARTIST1\"],",
				"  99,",
				"  \"\",",
				"  [1,\"★2\",\"TITLE2\",\"0\",\"ARTIST2\"],",
				"  {}",
				"];");
		assertEquals(2, l.size());
		assertEquals("TITLE1", l.get(0).getTitle());
		assertEquals("ARTIST1", l.get(0).getArtist());
		assertEquals(0, l.get(0).getLevelIndex());
		assertEquals("TITLE2", l.get(1).getTitle());
		assertEquals("ARTIST2", l.get(1).getArtist());
		assertEquals(1, l.get(1).getLevelIndex());
	}

	// [楽曲情報定義]
	// 配列の要素数が5個未満の項目は無視されること
	@Test
	public void testContent_NotEnoughElement() throws Exception {
		var l = parse(
				"var mname = [",
				"  [1,\"★1\",\"TITLE1\",\"0\",\"ARTIST1\"],",
				"  [1,\"★1\",\"TITLE1\",\"0\"],",
				"  [1,\"★2\",\"TITLE2\",\"0\",\"ARTIST2\",\"\",\"\"]",
				"];");
		assertEquals(2, l.size());
		assertEquals("TITLE1", l.get(0).getTitle());
		assertEquals("ARTIST1", l.get(0).getArtist());
		assertEquals(0, l.get(0).getLevelIndex());
		assertEquals("TITLE2", l.get(1).getTitle());
		assertEquals("ARTIST2", l.get(1).getArtist());
		assertEquals(1, l.get(1).getLevelIndex());
	}

	// [楽曲情報定義]
	// 難易度表記が記号で始まっていない項目は無視されること
	@Test
	public void testContent_LevelWithoutSymbol() throws Exception {
		var l = parse(
				"var mname = [",
				"  [1,\"★1\",\"TITLE1\",\"0\",\"ARTIST1\"],",
				"  [1,\"☆1\",\"TITLE1\",\"0\",\"ARTIST1\"]",
				"];");
		assertEquals(1, l.size());
		assertEquals("TITLE1", l.get(0).getTitle());
		assertEquals("ARTIST1", l.get(0).getArtist());
		assertEquals(0, l.get(0).getLevelIndex());
	}

	// [楽曲情報定義]
	// 該当する難易度ラベルが存在しない項目は無視されること
	@Test
	public void testContent_LevelNotFound() throws Exception {
		var l = parse(
				"var mname = [",
				"  [1,\"★1\",\"TITLE1\",\"0\",\"ARTIST1\"],",
				"  [1,\"★30\",\"TITLE1\",\"0\",\"ARTIST1\"]",
				"];");
		assertEquals(1, l.size());
		assertEquals("TITLE1", l.get(0).getTitle());
		assertEquals("ARTIST1", l.get(0).getArtist());
		assertEquals(0, l.get(0).getLevelIndex());
	}

	// [楽曲情報定義]
	// タイトルが空文字列の項目は無視されること
	@Test
	public void testContent_EmptyTitle() throws Exception {
		var l = parse(
				"var mname = [",
				"  [1,\"★1\",\"TITLE1\",\"0\",\"ARTIST1\"],",
				"  [1,\"★1\",\"\",\"0\",\"ARTIST1\"]",
				"];");
		assertEquals(1, l.size());
		assertEquals("TITLE1", l.get(0).getTitle());
		assertEquals("ARTIST1", l.get(0).getArtist());
		assertEquals(0, l.get(0).getLevelIndex());
	}

	// [楽曲情報定義]
	// 本体入手先URLが記述されていないアーティストは本体入手先URLがnull、アーティストが文字列全体になること
	@Test
	public void testContent_ArtistWithoutBody() throws Exception {
		var l = parse(
				"var mname = [",
				"  [1,\"★1\",\"A\",\"0\",\"No-Body-Artist\"]",
				"];");
		assertEquals("No-Body-Artist", l.get(0).getArtist());
		assertNull(l.get(0).getBodyUrl());
	}

	// [楽曲情報定義]
	// 本体入手先URLが正しい記述でない項目は本体入手先URLがnullになること
	@Test
	public void testContent_ArtistWithInvalidUrl() throws Exception {
		var l = parse(
				"var mname = [",
				"  [1,\"★1\",\"A\",\"0\",\"<a href='???'>Bad-URL-Artist</a>\"]",
				"];");
		assertEquals("Bad-URL-Artist", l.get(0).getArtist());
		assertNull(l.get(0).getBodyUrl());
	}

	private static List<ContentDescription> parse(String...src) throws Exception {
		return new GenocideHtmlParser().parse(
				Presets.GENOCIDE_INSANE.getTableDescription(),
				PlayStyle.SINGLE,
				Stream.of(src).collect(Collectors.joining("\n")).getBytes(Charset.forName("MS932")));
	}
}
