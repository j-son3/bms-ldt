package com.lmt.lib.bldt;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;

import org.junit.jupiter.api.Test;

public class ContentDescriptionTest {
	// ContentDescription(String, String, PlayStyle, int, URL, URL, String, String)
	// オブジェクトが正しく構築されること
	@Test
	public void testContentDescription_Normal() throws Exception {
		var d = new ContentDescription(
				"My Song",
				"Mr.X",
				PlayStyle.DOUBLE,
				10,
				new URL("http://localhost"),
				new URL("http://example.com"),
				"00112233445566778899aabbccddeeff",
				"00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
		assertEquals("My Song", d.getTitle());
		assertEquals("Mr.X", d.getArtist());
		assertEquals(PlayStyle.DOUBLE, d.getPlayStyle());
		assertEquals(10, d.getLevelIndex());
		assertEquals(new URL("http://localhost"), d.getBodyUrl());
		assertEquals(new URL("http://example.com"), d.getAdditionalUrl());
		assertEquals("00112233445566778899aabbccddeeff", d.getMd5());
		assertEquals("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff", d.getSha256());
	}

	// ContentDescription(String, String, PlayStyle, int, URL, URL, String, String)
	// null 指定可能な項目は null が指定できること
	@Test
	public void testContentDescription_NullableItems() throws Exception {
		var d = new ContentDescription(
				"Beautiful Music",
				"Unknown Composer",
				PlayStyle.SINGLE,
				50,
				null,
				null,
				null,
				null);
		assertEquals("Beautiful Music", d.getTitle());
		assertEquals("Unknown Composer", d.getArtist());
		assertEquals(PlayStyle.SINGLE, d.getPlayStyle());
		assertEquals(50, d.getLevelIndex());
		assertNull(d.getBodyUrl());
		assertNull(d.getAdditionalUrl());
		assertNull(d.getMd5());
		assertNull(d.getSha256());
	}

	// ContentDescription(String, String, PlayStyle, int, URL, URL, String, String)
	// NullPointerException title が null
	@Test
	public void testContentDescription_NullTitle() throws Exception {
		var ex = NullPointerException.class;
		assertThrows(ex, () -> new ContentDescription(null, "a", PlayStyle.SINGLE, 0, null, null, null, null));
	}

	// ContentDescription(String, String, PlayStyle, int, URL, URL, String, String)
	// NullPointerException artist が null
	@Test
	public void testContentDescription_NullArtist() throws Exception {
		var ex = NullPointerException.class;
		assertThrows(ex, () -> new ContentDescription("t", null, PlayStyle.SINGLE, 0, null, null, null, null));
	}

	// ContentDescription(String, String, PlayStyle, int, URL, URL, String, String)
	// NullPointerException playStyle が null
	@Test
	public void testContentDescription_NullPlayStyle() throws Exception {
		var ex = NullPointerException.class;
		assertThrows(ex, () -> new ContentDescription("t", "a", null, 0, null, null, null, null));
	}

	// ContentDescription(String, String, PlayStyle, int, URL, URL, String, String)
	// IllegalArgumentException title が空文字列
	@Test
	public void testContentDescription_EmptyTitle() throws Exception {
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> new ContentDescription("", "a", PlayStyle.SINGLE, 0, null, null, null, null));
	}

	// ContentDescription(String, String, PlayStyle, int, URL, URL, String, String)
	// IllegalArgumentException levelIndex が負の値
	@Test
	public void testContentDescription_NegativeLevelIndex() throws Exception {
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> new ContentDescription("t", "a", PlayStyle.SINGLE, -1, null, null, null, null));
	}

	// ContentDescription(String, String, PlayStyle, int, URL, URL, String, String)
	// IllegalArgumentException md5 がMD5の形式ではない
	@Test
	public void testContentDescription_InvalidMd5() throws Exception {
		var ex = IllegalArgumentException.class;
		var v1 = "00112233445566778899aabbccddeeGG";
		assertThrows(ex, () -> new ContentDescription("t", "a", PlayStyle.SINGLE, 0, null, null, v1, null));
		var v2 = "00112233445566778899aabbccddeeff00";
		assertThrows(ex, () -> new ContentDescription("t", "a", PlayStyle.SINGLE, 0, null, null, v2, null));
		var v3 = "00112233445566778899aabbccddee";
		assertThrows(ex, () -> new ContentDescription("t", "a", PlayStyle.SINGLE, 0, null, null, v3, null));
		var v4 = " 00112233445566778899aabbccddeeff ";
		assertThrows(ex, () -> new ContentDescription("t", "a", PlayStyle.SINGLE, 0, null, null, v4, null));
	}

	// ContentDescription(String, String, PlayStyle, int, URL, URL, String, String)
	// IllegalArgumentException sha256 がSHA-256の形式ではない
	@Test
	public void testContentDescription_InvalidSha256() throws Exception {
		var ex = IllegalArgumentException.class;
		var v1 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeGG";
		assertThrows(ex, () -> new ContentDescription("t", "a", PlayStyle.SINGLE, 0, null, null, null, v1));
		var v2 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00";
		assertThrows(ex, () -> new ContentDescription("t", "a", PlayStyle.SINGLE, 0, null, null, null, v2));
		var v3 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddee";
		assertThrows(ex, () -> new ContentDescription("t", "a", PlayStyle.SINGLE, 0, null, null, null, v3));
		var v4 = " 00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff ";
		assertThrows(ex, () -> new ContentDescription("t", "a", PlayStyle.SINGLE, 0, null, null, null, v4));
	}

	// getTitle()
	// 正しい値を返すこと
	@Test
	public void testGetTitle() {
		// Do nothing: コンストラクタで確認済み
	}

	// getArtist()
	// 正しい値を返すこと
	@Test
	public void testGetArtist() {
		// Do nothing: コンストラクタで確認済み
	}

	// getPlayStyle()
	// 正しい値を返すこと
	@Test
	public void testGetPlayStyle() {
		// Do nothing: コンストラクタで確認済み
	}

	// getLevelIndex()
	// 正しい値を返すこと
	@Test
	public void testGetLevelIndex() {
		// Do nothing: コンストラクタで確認済み
	}

	// getBodyUrl()
	// 正しい値を返すこと
	@Test
	public void testGetBodyUrl() {
		// Do nothing: コンストラクタで確認済み
	}

	// getAdditionalUrl()
	// 正しい値を返すこと
	@Test
	public void testGetAdditionalUrl() {
		// Do nothing: コンストラクタで確認済み
	}

	// getMd5()
	// 正しい値を返すこと
	@Test
	public void testGetMd5() {
		// Do nothing: コンストラクタで確認済み
	}

	// getSha256()
	// 正しい値を返すこと
	@Test
	public void testGetSha256() {
		// Do nothing: コンストラクタで確認済み
	}
}
