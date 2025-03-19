package com.lmt.lib.bldt;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

public class PlayStyleDescriptionTest {
	// PlayStyleDescription(String, URL, Collection<String>)
	// オブジェクトが正しく構築されること
	@Test
	public void testPlayStyleDescription_Normal() throws Exception {
		var d = new PlayStyleDescription("A", new URL("https://www.lm-t.com/"), List.of("1", "2", "3"));
		assertEquals("A", d.getSymbol());
		assertEquals(new URL("https://www.lm-t.com/"), d.getContentUrl());
		assertEquals(List.of("1", "2", "3"), d.getLabels());
	}

	// PlayStyleDescription(String, URL, Collection<String>)
	// NullPointerException symbol が null
	@Test
	public void testPlayStyleDescription_NullSymbol() throws Exception {
		var ex = NullPointerException.class;
		assertThrows(ex, () -> new PlayStyleDescription(null, new URL("https://www.lm-t.com/"), List.of("1")));
	}

	// PlayStyleDescription(String, URL, Collection<String>)
	// NullPointerException contentUrl が null
	@Test
	public void testPlayStyleDescription_NullContentUrl() throws Exception {
		var ex = NullPointerException.class;
		assertThrows(ex, () -> new PlayStyleDescription("A", null, List.of("1")));
	}

	// PlayStyleDescription(String, URL, Collection<String>)
	// NullPointerException labels が null
	@Test
	public void testPlayStyleDescription_NullLabels() throws Exception {
		var ex = NullPointerException.class;
		assertThrows(ex, () -> new PlayStyleDescription(null, new URL("https://www.lm-t.com/"), null));
	}

	// PlayStyleDescription(String, URL, Collection<String>)
	// IllegalArgumentException symbol が空文字列
	@Test
	public void testPlayStyleDescription_EmptySymbol() throws Exception {
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> new PlayStyleDescription("", new URL("https://www.lm-t.com/"), List.of("1")));
	}

	// PlayStyleDescription(String, URL, Collection<String>)
	// IllegalArgumentException labels が0件
	@Test
	public void testPlayStyleDescription_EmptyLabels() throws Exception {
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> new PlayStyleDescription("A", new URL("https://www.lm-t.com/"), List.of()));
	}

	// PlayStyleDescription(String, URL, Collection<String>)
	// IllegalArgumentException labels の中に空文字列が含まれている
	@Test
	public void testPlayStyleDescription_HasEmptyInLabels() throws Exception {
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> new PlayStyleDescription("A", new URL("https://www.lm-t.com/"), List.of("1", "")));
	}

	// PlayStyleDescription(String, URL, Collection<String>)
	// IllegalArgumentException labels の中に同じ文字列が含まれている
	@Test
	public void testPlayStyleDescription_ConflictLabels() throws Exception {
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> new PlayStyleDescription("A", new URL("https://www.lm-t.com/"), List.of("1", "2", "1")));
	}

	// getSymbol()
	// 正しい値を返すこと
	@Test
	public void testGetSymbol() throws Exception {
		// Do nothing: コンストラクタで確認済み
	}

	// getContentUrl()
	// 正しい値を返すこと
	@Test
	public void testGetContentUrl() throws Exception {
		// Do nothing: コンストラクタで確認済み
	}


	// getLabels()
	// 正しい値を返すこと
	@Test
	public void testGetLabels_Normal() throws Exception {
		// Do nothing: コンストラクタで確認済み
	}

	// getLabels()
	// リストが読み取り専用であること
	@Test
	public void testGetLabels_ReadOnly() throws Exception {
		var d = new PlayStyleDescription("A", new URL("https://www.lm-t.com/"), List.of("1", "2", "3"));
		var labels = d.getLabels();
		var ex = UnsupportedOperationException.class;
		assertThrows(ex, () -> labels.add("ADD"));
	}

	// getLevelIndex(String)
	// 存在する難易度ラベルのインデックス値を返すこと
	@Test
	public void testGetLevelIndex_Found() throws Exception {
		var d = new PlayStyleDescription("A", new URL("https://www.lm-t.com/"), List.of("3", "1", "2"));
		assertEquals(0, d.getLevelIndex("3"));
		assertEquals(1, d.getLevelIndex("1"));
		assertEquals(2, d.getLevelIndex("2"));
	}

	// getLevelIndex(String)
	// 存在しに難易度ラベルでは-1を返すこと
	@Test
	public void testGetLevelIndex_NotFound() throws Exception {
		var d = new PlayStyleDescription("A", new URL("https://www.lm-t.com/"), List.of("3", "1", "2"));
		assertEquals(-1, d.getLevelIndex("NOT_FOUND"));
	}

	// getLevelIndex(String)
	// NullPointerException label が null
	@Test
	public void testGetLevelIndex_NullLabel() throws Exception {
		var d = new PlayStyleDescription("A", new URL("https://www.lm-t.com/"), List.of("3", "1", "2"));
		var ex = NullPointerException.class;
		assertThrows(ex, () -> d.getLevelIndex(null));
	}
}
