package com.lmt.lib.bldt;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lmt.lib.bldt.parser.GenocideHtmlParser;

public class TableDescriptionTest {
	static final Parser EMPTY_PARSER = new Parser() {
		@Override
		public List<ContentDescription> parse(TableDescription tableDesc, PlayStyle playStyle, byte[] raw)
				throws IOException {
			return List.of();
		}
	};

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// オブジェクトが正しく構築されること
	@Test
	public void testTableDescription_Normal() throws Exception {
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		var dp = new PlayStyleDescription("d", new URL("http://example.com/dp/"), List.of("1d"));
		var d = new TableDescription(
				"my_Difficulty_Table_123",
				"My Difficulty Table",
				new URL("http://example.com/home/"),
				new GenocideHtmlParser(),
				sp,
				dp);
		assertEquals("my_Difficulty_Table_123", d.getId());
		assertEquals("My Difficulty Table", d.getName());
		assertEquals(new URL("http://example.com/home/"), d.getOfficialUrl());
		assertEquals(GenocideHtmlParser.class, d.getParser().getClass());
		assertSame(sp, d.getSingleDescription());
		assertSame(dp, d.getDoubleDescription());
	}

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// シングルプレーのみ対応のオブジェクトが構築できること
	@Test
	public void testTableDescription_SpOnly() throws Exception {
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		var d = new TableDescription("id", "name", new URL("http://a"), EMPTY_PARSER, sp, null);
		assertSame(sp, d.getSingleDescription());
		assertNull(d.getDoubleDescription());
	}

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// ダブルプレーのみ対応のオブジェクトが構築できること
	@Test
	public void testTableDescription_DpOnly() throws Exception {
		var dp = new PlayStyleDescription("d", new URL("http://example.com/dp/"), List.of("1d"));
		var d = new TableDescription("id", "name", new URL("http://a"), EMPTY_PARSER, null, dp);
		assertNull(d.getSingleDescription());
		assertSame(dp, d.getDoubleDescription());
	}

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// NullPointerException id が null
	@Test
	public void testTableDescription_NullId() throws Exception {
		var ex = NullPointerException.class;
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		assertThrows(ex, () -> new TableDescription(null, "b", new URL("http://a"), EMPTY_PARSER, sp, null));
	}

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// NullPointerException name が null
	@Test
	public void testTableDescription_NullName() throws Exception {
		var ex = NullPointerException.class;
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		assertThrows(ex, () -> new TableDescription("a", null, new URL("http://a"), EMPTY_PARSER, sp, null));
	}

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// NullPointerException officialUrl が null
	@Test
	public void testTableDescription_NullOfficialUrl() throws Exception {
		var ex = NullPointerException.class;
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		assertThrows(ex, () -> new TableDescription("a", "b", null, EMPTY_PARSER, sp, null));
	}

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// NullPointerException parser が null
	@Test
	public void testTableDescription_NullParser() throws Exception {
		var ex = NullPointerException.class;
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		assertThrows(ex, () -> new TableDescription("a", "b", new URL("http://a"), null, sp, null));
	}

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// IllegalArgumentException IDの形式が不正
	@Test
	public void testTableDescription_InvalidId() throws Exception {
		var ex = IllegalArgumentException.class;
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		assertThrows(ex, () -> new TableDescription("", "b", new URL("http://a"), EMPTY_PARSER, sp, null));
		assertThrows(ex, () -> new TableDescription("1a", "b", new URL("http://a"), EMPTY_PARSER, sp, null));
		assertThrows(ex, () -> new TableDescription("z#", "b", new URL("http://a"), EMPTY_PARSER, sp, null));
		assertThrows(ex, () -> new TableDescription("#z", "b", new URL("http://a"), EMPTY_PARSER, sp, null));
		assertThrows(ex, () -> new TableDescription("abc!", "b", new URL("http://a"), EMPTY_PARSER, sp, null));
		assertThrows(ex, () -> new TableDescription("ａ", "b", new URL("http://a"), EMPTY_PARSER, sp, null));
	}

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// IllegalArgumentException 難易度表名称が空文字列
	@Test
	public void testTableDescription_EmptyName() throws Exception {
		var ex = IllegalArgumentException.class;
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		assertThrows(ex, () -> new TableDescription("a", "", new URL("http://a"), EMPTY_PARSER, sp, null));
	}

	// TableDescription(String, String, URL, Parser, PlayStyleDescription, PlayStyleDescription)
	// IllegalArgumentException プレースタイル定義が全てnull
	@Test
	public void testTableDescription_AllNullPlayStyle() throws Exception {
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> new TableDescription("a", "b", new URL("http://a"), EMPTY_PARSER, null, null));
	}

	// getId()
	// 正しい値を返すこと
	@Test
	public void testGetId() {
		// Do nothing: コンストラクタで確認済み
	}

	// getName()
	// 正しい値を返すこと
	@Test
	public void testGetName() {
		// Do nothing: コンストラクタで確認済み
	}

	// getOfficialUrl()
	// 正しい値を返すこと
	@Test
	public void testGetOfficialUrl() {
		// Do nothing: コンストラクタで確認済み
	}

	// getParser()
	// 正しい値を返すこと
	@Test
	public void testGetParser() {
		// Do nothing: コンストラクタで確認済み
	}

	// getPlayStyleDescription(PlayStyle)
	// 正しい値を返すこと
	@Test
	public void testGetPlayStyleDescription_Normal() throws Exception {
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		var dp = new PlayStyleDescription("d", new URL("http://example.com/dp/"), List.of("1d"));
		var d = new TableDescription("a", "b", new URL("http://a"), EMPTY_PARSER, sp, dp);
		assertSame(sp, d.getPlayStyleDescription(PlayStyle.SINGLE));
		assertSame(dp, d.getPlayStyleDescription(PlayStyle.DOUBLE));
	}

	// getPlayStyleDescription(PlayStyle)
	// NullPointerException testPlayStyleDescription_ が null
	@Test
	public void testGetPlayStyleDescription_NullPlayStyle() throws Exception {
		var ex = NullPointerException.class;
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		var d = new TableDescription("a", "b", new URL("http://a"), EMPTY_PARSER, sp, null);
		assertThrows(ex, () -> d.getPlayStyleDescription(null));
	}

	// getSingleDescription()
	// 正しい値を返すこと
	@Test
	public void testGetSingleDescription() {
		// Do nothing: コンストラクタで確認済み
	}

	// getDoubleDescription()
	// 正しい値を返すこと
	@Test
	public void testGetDoubleDescription() {
		// Do nothing: コンストラクタで確認済み
	}

	// isPreset()
	// 正しい値を返すこと
	@Test
	public void testIsPreset() throws Exception {
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		var d = new TableDescription("a", "b", new URL("http://a"), EMPTY_PARSER, sp, null);
		d.setIsPreset(false);
		assertFalse(d.isPreset());
		d.setIsPreset(true);
		assertTrue(d.isPreset());
	}
}
