package com.lmt.lib.bldt;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lmt.lib.bldt.parser.GenocideHtmlParser;
import com.lmt.lib.bldt.parser.ScoreJsonParser;

public class PresetsTest {
	// NOTICE：プリセット難易度表のアサーションでは、難易度表名称の検証は行わない。

	// GENOCIDE通常：難易度表定義が期待通りであること
	@Test
	public void testGenocideNormal() throws Exception {
		var preset = Presets.GENOCIDE_NORMAL;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("genocide_n", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://nekokan.dyndns.info/~lobsak/genocide/"), desc.getOfficialUrl());
		assertEquals(GenocideHtmlParser.class, desc.getParser().getClass());
		assertEquals("☆", sp.getSymbol());
		assertEquals(new URL("https://nekokan.dyndns.info/~lobsak/genocide/normal.html"), sp.getContentUrl());
		assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "X"), sp.getLabels());
		assertNull(dp);
	}

	// GENOCIDE発狂：難易度表定義が期待通りであること
	@Test
	public void testGenocideInsane() throws Exception {
		var preset = Presets.GENOCIDE_INSANE;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("genocide_i", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://nekokan.dyndns.info/~lobsak/genocide/"), desc.getOfficialUrl());
		assertEquals(GenocideHtmlParser.class, desc.getParser().getClass());
		assertEquals("★", sp.getSymbol());
		assertEquals(new URL("https://nekokan.dyndns.info/~lobsak/genocide/insane.html"), sp.getContentUrl());
		assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
				"17", "18", "19", "20", "21", "22", "23", "24", "25", "???"), sp.getLabels());
		assertNull(dp);
	}

	// δ通常：難易度表定義が期待通りであること
	@Test
	public void testDeltaNormal() throws Exception {
		var preset = Presets.DELTA_NORMAL;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("delta_n", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://deltabms.yaruki0.net/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertNull(sp);
		assertEquals("δ", dp.getSymbol());
		assertEquals(new URL("https://deltabms.yaruki0.net/table/data/dpdelta_data.json"), dp.getContentUrl());
		assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11",
				"97", "98", "99"), dp.getLabels());
	}

	// δ発狂：難易度表定義が期待通りであること
	@Test
	public void testDeltaInsane() throws Exception {
		var preset = Presets.DELTA_INSANE;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("delta_i", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://deltabms.yaruki0.net/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertNull(sp);
		assertEquals("★", dp.getSymbol());
		assertEquals(new URL("https://deltabms.yaruki0.net/table/data/insane_data.json"), dp.getContentUrl());
		assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "?"), dp.getLabels());
	}

	// NEW GENERATION通常：難易度表定義が期待通りであること
	@Test
	public void testNewGenerationNormal() throws Exception {
		var preset = Presets.NEW_GENERATION_NORMAL;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("newgen_n", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://rattoto10.jounin.jp/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("▽", sp.getSymbol());
		assertEquals(new URL("https://rattoto10.github.io/second_table/score.json"), sp.getContentUrl());
		assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "11+",
				"12-", "12", "12+", "？"), sp.getLabels());
		assertNull(dp);
	}

	// NEW GENERATION発狂：難易度表定義が期待通りであること
	@Test
	public void testNewGenerationInsane() throws Exception {
		var preset = Presets.NEW_GENERATION_INSANE;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("newgen_i", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://rattoto10.jounin.jp/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("▼", sp.getSymbol());
		assertEquals(new URL("https://rattoto10.github.io/second_table/insane_data.json"), sp.getContentUrl());
		assertEquals(List.of("0-", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
				"15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "?"), sp.getLabels());
		assertNull(dp);
	}

	// Satellite：難易度表定義が期待通りであること
	@Test
	public void testSatellite() throws Exception {
		var preset = Presets.SATELLITE;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("satellite", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://stellabms.xyz/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("sl", sp.getSymbol());
		assertEquals(new URL("https://stellabms.xyz/sl/score.json"), sp.getContentUrl());
		assertEquals(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"), sp.getLabels());
		assertEquals("DPsl", dp.getSymbol());
		assertEquals(new URL("https://stellabms.xyz/dp/score.json"), dp.getContentUrl());
		assertEquals(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), dp.getLabels());
	}

	// Stella：難易度表定義が期待通りであること
	@Test
	public void testStella() throws Exception {
		var preset = Presets.STELLA;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("stella", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://stellabms.xyz/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("st", sp.getSymbol());
		assertEquals(new URL("https://stellabms.xyz/st/score.json"), sp.getContentUrl());
		assertEquals(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"), sp.getLabels());
		assertEquals("DPst", dp.getSymbol());
		assertEquals(new URL("https://stellabms.xyz/dpst/score.json"), dp.getContentUrl());
		assertEquals(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), dp.getLabels());
	}

	// Solar：難易度表定義が期待通りであること
	@Test
	public void testSolar() throws Exception {
		var preset = Presets.SOLAR;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("solar", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://stellabms.xyz/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("so", sp.getSymbol());
		assertEquals(new URL("https://stellabms.xyz/so/score.json"), sp.getContentUrl());
		assertEquals(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"), sp.getLabels());
		assertNull(dp);
	}

	// Supernova：難易度表定義が期待通りであること
	@Test
	public void testSupernova() throws Exception {
		var preset = Presets.SUPERNOVA;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("supernova", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://stellabms.xyz/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("sn", sp.getSymbol());
		assertEquals(new URL("https://stellabms.xyz/sn/score.json"), sp.getContentUrl());
		assertEquals(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"), sp.getLabels());
		assertNull(dp);
	}

	// Overjoy：難易度表定義が期待通りであること
	@Test
	public void testOverjoy() throws Exception {
		var preset = Presets.OVERJOY;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("overjoy", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://rattoto10.jounin.jp/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("★★", sp.getSymbol());
		assertEquals(new URL("https://rattoto10.github.io/second_table/overjoy_score.json"), sp.getContentUrl());
		assertEquals(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8"), sp.getLabels());
		assertEquals("★★", dp.getSymbol());
		assertEquals(new URL("http://ereter.net/static/analyzer/json/overjoy.json"), dp.getContentUrl());
		assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "99"), dp.getLabels());
	}

	// LN：難易度表定義が期待通りであること
	@Test
	public void testLongNote() throws Exception {
		var preset = Presets.LONG_NOTE;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("long_note", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("http://flowermaster.web.fc2.com/lrnanido/bmsln.html"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("◆", sp.getSymbol());
		assertEquals(new URL("http://flowermaster.web.fc2.com/lrnanido/gla/score.json"), sp.getContentUrl());
		assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
				"17", "18", "19", "20", "21", "22", "23", "24", "25", "26"), sp.getLabels());
		assertNull(dp);
	}

	// Scramble：難易度表定義が期待通りであること
	@Test
	public void testScramble() throws Exception {
		var preset = Presets.SCRAMBLE;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("scramble", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://egret9.github.io/Scramble/"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("SB", sp.getSymbol());
		assertEquals(new URL("https://script.google.com/macros/s/AKfycbw5pnMwlCFZz7wDY5kRsBpfSm0-luKszs8LQAEE6BKkVT1R78-CpB4WA9chW-gdBsF7IA/exec"), sp.getContentUrl());
		assertEquals(List.of("-1", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"), sp.getLabels());
		assertNull(dp);
	}

	// 癖譜面ライブラリー：難易度表定義が期待通りであること
	@Test
	public void testUnique() throws Exception {
		var preset = Presets.UNIQUE;
		var id = preset.getId();
		var desc = preset.getTableDescription();
		var sp = desc.getSingleDescription();
		var dp = desc.getDoubleDescription();
		assertEquals("unique", id);
		assertEquals(id, desc.getId());
		assertEquals(new URL("https://rattoto10.web.fc2.com/kuse_library/main.html"), desc.getOfficialUrl());
		assertEquals(ScoreJsonParser.class, desc.getParser().getClass());
		assertEquals("癖", sp.getSymbol());
		assertEquals(new URL("https://rattoto10.web.fc2.com/kuse_library/score.json"), sp.getContentUrl());
		assertEquals(List.of("0", "1" ,"2" ,"3" ,"4" ,"5" ,"6" ,"7" ,"8" ,"9" , "10", "11", "12", "13", "14", "15",
				"16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "51",
				"52", "53", "54", "55", "56", "99"), sp.getLabels());
		assertNull(dp);
	}
}
