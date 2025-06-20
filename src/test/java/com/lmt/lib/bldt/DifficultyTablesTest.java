package com.lmt.lib.bldt;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lmt.lib.bldt.parser.ScoreJsonParser;

public class DifficultyTablesTest {
	private Map<String, TableDescription> mOrgTableDescs;

	@BeforeEach
	public void beforeEach() throws Exception {
		mOrgTableDescs = new LinkedHashMap<>(Tests.getsf(DifficultyTables.class, "sTableDescs"));
	}

	@AfterEach
	public void afterEach() throws Exception {
		Tests.setsf(DifficultyTables.class, "sTableDescs", mOrgTableDescs);
		DifficultyTables.setLogger(null);
	}

	// all()
	// デフォルトではプリセットの定義順で難易度表定義が走査されること
	@Test
	public void testAll_Default() {
		var all = DifficultyTables.all().collect(Collectors.toList());
		var presets = Stream.of(Presets.values()).map(p -> p.getTableDescription()).collect(Collectors.toList());
		assertEquals(presets.size(), all.size());
		for (var i = 0; i < all.size(); i++) {
			assertSame(all.get(i), presets.get(i));
		}
	}

	// all()
	// 難易度表定義を追加するとプリセットに続いて追加した難易度表定義が追加順に走査されること
	@Test
	public void testAll_Added() throws Exception {
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		var d1 = new TableDescription("a1", "b", new URL("http://a"), new ScoreJsonParser(), sp, null);
		var d2 = new TableDescription("a2", "b", new URL("http://a"), new ScoreJsonParser(), sp, null);
		DifficultyTables.add(d1);
		DifficultyTables.add(d2);
		var all = DifficultyTables.all().collect(Collectors.toList());
		assertSame(d1, all.get(all.size() - 2));
		assertSame(d2, all.get(all.size() - 1));
	}

	// get(String)
	// プリセットの難易度表定義が全て取得できること
	@Test
	public void testGet_Presets() {
		for (var preset : Presets.values()) {
			var d = DifficultyTables.get(preset.getId());
			assertAll(
					() -> assertNotNull(d),
					() -> assertTrue(d.isPreset()),
					() -> assertEquals(d.getId(), preset.getId()));
		}
	}

	// get(String)
	// 追加した難易度表定義も取得できること
	@Test
	public void testGet_Added() throws Exception {
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		var d1 = new TableDescription("a1", "b", new URL("http://a"), new ScoreJsonParser(), sp, null);
		var d2 = new TableDescription("a2", "b", new URL("http://a"), new ScoreJsonParser(), sp, null);
		DifficultyTables.add(d1);
		DifficultyTables.add(d2);
		assertSame(d1, DifficultyTables.get("a1"));
		assertSame(d2, DifficultyTables.get("a2"));
	}

	// get(String)
	// 存在しないIDを指定すると null を返すこと
	@Test
	public void testGet_NotFound() {
		assertNull(DifficultyTables.get("GENOCIDE_NORMAL"));
		assertNull(DifficultyTables.get("Genocide_normal"));
		assertNull(DifficultyTables.get("genocide_normal_"));
		assertNull(DifficultyTables.get(""));
	}

	// get(String)
	// NullPointerException idがnull
	@Test
	public void testGet_NullId() {
		assertThrows(NullPointerException.class, () -> DifficultyTables.get(null));
	}

	// add(TableDescription)
	// 難易度表定義が正しく追加されること
	@Test
	public void testAdd_Normal() {
		// Do nothing: 他のテストケースで試験済み
	}

	// add(TableDescription)
	// NullPointerException tableDescがnull
	@Test
	public void testAdd_NullTableDesc() {
		assertThrows(NullPointerException.class, () -> DifficultyTables.add(null));
	}

	// add(TableDescription)
	// IllegalArgumentException IDが競合している
	@Test
	public void testAdd_ConflictId() throws Exception {
		var sp = new PlayStyleDescription("s", new URL("http://example.com/sp/"), List.of("1s"));
		var badId = Presets.GENOCIDE_NORMAL.getId();
		var d = new TableDescription(badId, "b", new URL("http://a"), new ScoreJsonParser(), sp, null);
		assertThrows(IllegalArgumentException.class, () -> DifficultyTables.add(d));
	}

	// printLog(String)
	// 設定されたロガーに期待するメッセージが転送されること
	@Test
	public void testPrintLog1_Normal() {
		var called = new AtomicBoolean(false);
		var msg = "This is log message";
		var ptn = "^\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ \\[DEBUG\\] " + msg + "$";
		DifficultyTables.setLogger(s -> { assertTrue(s.matches(ptn)); called.set(true); });
		DifficultyTables.printLog(msg);
		assertTrue(called.get());
	}

	// printLog(String)
	// ロガーが未設定だと何も起きないこと
	@Test
	public void testPrintLog1_NoLogger() {
		DifficultyTables.setLogger(null);
		DifficultyTables.printLog("a");
	}

	// printLog(String, Object...)
	// 設定されたロガーに期待するメッセージが転送されること
	@Test
	public void testPrintLog2_Normal() {
		var called = new AtomicBoolean(false);
		var ptn = "^\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+ \\[DEBUG\\] Value=100$";
		DifficultyTables.setLogger(s -> { assertTrue(s.matches(ptn)); called.set(true); });
		DifficultyTables.printLog("Value=%d", 100);
		assertTrue(called.get());
	}

	// printLog(String, Object...)
	// ロガーが未設定だと何も起きないこと
	@Test
	public void testPrintLog2_NoLogger() {
		DifficultyTables.setLogger(null);
		DifficultyTables.printLog("str=%s", "aaa");
	}

	// setLogger(Consumer<String>)
	// ロガーが正しく設定されること
	@Test
	public void testSetLogger_Normal() throws Exception {
		Consumer<String> logger = s -> {};
		DifficultyTables.setLogger(logger);
		assertSame(logger, Tests.getsf(DifficultyTables.class, "sLogger"));
	}

	// setLogger(Consumer<String>)
	// null を指定可能であること
	@Test
	public void testSetLogger_Null() throws Exception {
		DifficultyTables.setLogger(null);
		assertNull(Tests.getsf(DifficultyTables.class, "sLogger"));
	}

	// setLocale(Locale)
	// プリセットの難易度表名称がロケールに対応する名称に切り替わること(日本語)
	@Test
	public void testSetLocale_Ja() {
		DifficultyTables.setLocale(Locale.JAPANESE);
		assertEquals("GENOCIDE通常", Presets.GENOCIDE_NORMAL.getTableDescription().getName());
		assertEquals("GENOCIDE発狂", Presets.GENOCIDE_INSANE.getTableDescription().getName());
		assertEquals("δ通常", Presets.DELTA_NORMAL.getTableDescription().getName());
		assertEquals("δ発狂", Presets.DELTA_INSANE.getTableDescription().getName());
		assertEquals("NEW GENERATION通常", Presets.NEW_GENERATION_NORMAL.getTableDescription().getName());
		assertEquals("NEW GENERATION発狂", Presets.NEW_GENERATION_INSANE.getTableDescription().getName());
		assertEquals("Satellite", Presets.SATELLITE.getTableDescription().getName());
		assertEquals("Stella", Presets.STELLA.getTableDescription().getName());
		assertEquals("Overjoy", Presets.OVERJOY.getTableDescription().getName());
		assertEquals("LN", Presets.LONG_NOTE.getTableDescription().getName());
		assertEquals("Scramble", Presets.SCRAMBLE.getTableDescription().getName());
		assertEquals("癖譜面ライブラリー", Presets.UNIQUE.getTableDescription().getName());
	}

	// setLocale(Locale)
	// プリセットの難易度表名称がロケールに対応する名称に切り替わること(韓国語)
	@Test
	public void testSetLocale_Ko() {
		DifficultyTables.setLocale(Locale.KOREAN);
		assertEquals("GENOCIDE 보통", Presets.GENOCIDE_NORMAL.getTableDescription().getName());
		assertEquals("GENOCIDE 미치", Presets.GENOCIDE_INSANE.getTableDescription().getName());
		assertEquals("δ 보통", Presets.DELTA_NORMAL.getTableDescription().getName());
		assertEquals("δ 미치", Presets.DELTA_INSANE.getTableDescription().getName());
		assertEquals("NEW GENERATION 보통", Presets.NEW_GENERATION_NORMAL.getTableDescription().getName());
		assertEquals("NEW GENERATION 미친", Presets.NEW_GENERATION_INSANE.getTableDescription().getName());
		assertEquals("Satellite", Presets.SATELLITE.getTableDescription().getName());
		assertEquals("Stella", Presets.STELLA.getTableDescription().getName());
		assertEquals("Overjoy", Presets.OVERJOY.getTableDescription().getName());
		assertEquals("LN", Presets.LONG_NOTE.getTableDescription().getName());
		assertEquals("Scramble", Presets.SCRAMBLE.getTableDescription().getName());
		assertEquals("버릇 악보 라이브러리", Presets.UNIQUE.getTableDescription().getName());
	}

	// setLocale(Locale)
	// 非対応の言語を指定するとプリセットの難易度表名称が英語に切り替わること
	@Test
	public void testSetLocale_Unknown() {
		var loc = Locale.getDefault();
		try {
			Locale.setDefault(Locale.ROOT);
			DifficultyTables.setLocale(Locale.GERMAN);
			assertEquals("GENOCIDE Normal", Presets.GENOCIDE_NORMAL.getTableDescription().getName());
			assertEquals("GENOCIDE Insane", Presets.GENOCIDE_INSANE.getTableDescription().getName());
			assertEquals("Delta Normal", Presets.DELTA_NORMAL.getTableDescription().getName());
			assertEquals("Delta Insane", Presets.DELTA_INSANE.getTableDescription().getName());
			assertEquals("NEW GENERATION Normal", Presets.NEW_GENERATION_NORMAL.getTableDescription().getName());
			assertEquals("NEW GENERATION Insane", Presets.NEW_GENERATION_INSANE.getTableDescription().getName());
			assertEquals("Satellite", Presets.SATELLITE.getTableDescription().getName());
			assertEquals("Stella", Presets.STELLA.getTableDescription().getName());
			assertEquals("Overjoy", Presets.OVERJOY.getTableDescription().getName());
			assertEquals("LN", Presets.LONG_NOTE.getTableDescription().getName());
			assertEquals("Scramble", Presets.SCRAMBLE.getTableDescription().getName());
			assertEquals("Unique Chart Library", Presets.UNIQUE.getTableDescription().getName());
		} finally {
			Locale.setDefault(loc);
		}
	}

	// setLocale(Locale)
	// NullPointerException localeがnull
	@Test
	public void testSetLocale_NullLocale() {
		assertThrows(NullPointerException.class, () -> DifficultyTables.setLocale(null));
	}
}
