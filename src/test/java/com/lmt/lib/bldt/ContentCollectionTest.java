package com.lmt.lib.bldt;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class ContentCollectionTest {
	private static TableDescription TD = Presets.GENOCIDE_INSANE.getTableDescription();
	private static ZonedDateTime ZDT = ZonedDateTime.now();
	private static String HASH = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
	private static String SHA256_1 = "1111111111111111111111111111111111111111111111111111111111111111";
	private static String SHA256_2 = "2222222222222222222222222222222222222222222222222222222222222222";
	private static String SHA256_3 = "3333333333333333333333333333333333333333333333333333333333333333";
	private static String SHA256_X = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
	private static String MD5_1 = "11111111111111111111111111111111";
	private static String MD5_2 = "22222222222222222222222222222222";
	private static String MD5_3 = "33333333333333333333333333333333";
	//private static String MD5_X = "ffffffffffffffffffffffffffffffff";

	// ContentCollection(TableDescription, ZonedDateTime, ZonedDateTime, String, ZonedDateTime, String, Collection<ContentDescription>)
	// オブジェクトが正しく構築されること
	@Test
	public void testContentCollection_Normal() throws Exception {
		var desc = Presets.SATELLITE.getTableDescription();
		var udt = ZonedDateTime.now();
		var spMdt = ZonedDateTime.parse("2025-01-02T03:04:05.600Z");
		var spMdh = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
		var dpMdt = ZonedDateTime.parse("2025-06-05T04:03:02.100Z");
		var dpMdh = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
		var cd1 = new ContentDescription("t1", "a1", PlayStyle.SINGLE, 0, null, null, null, null);
		var cd2 = new ContentDescription("t2", "a2", PlayStyle.SINGLE, 0, null, null, null, null);
		var cd3 = new ContentDescription("t3", "a3", PlayStyle.SINGLE, 0, null, null, null, null);
		var contents = List.of(cd1, cd2, cd3);
		var cc = new ContentCollection(desc, udt, spMdt, spMdh, dpMdt, dpMdh, contents);
		assertSame(desc, cc.getTableDescription());
		assertEquals(udt, cc.getLastUpdateDateTime());
		assertEquals(spMdt, cc.getModifiedDateTime(PlayStyle.SINGLE));
		assertEquals(spMdh, cc.getModifiedDataHash(PlayStyle.SINGLE));
		assertEquals(dpMdt, cc.getModifiedDateTime(PlayStyle.DOUBLE));
		assertEquals(dpMdh, cc.getModifiedDataHash(PlayStyle.DOUBLE));
		assertEquals(3, cc.getCount());
		assertSame(cd1, cc.get(0));
		assertSame(cd2, cc.get(1));
		assertSame(cd3, cc.get(2));
	}

	// ContentCollection(TableDescription, ZonedDateTime, ZonedDateTime, String, ZonedDateTime, String, Collection<ContentDescription>)
	// 任意項目に null を指定可、楽曲情報リスト0件が許容されること
	@Test
	public void testContentCollection_Optionals() throws Exception {
		var cc = new ContentCollection(TD, ZDT, null, null, null, null, List.of());
		assertNull(cc.getModifiedDateTime(PlayStyle.SINGLE));
		assertNull(cc.getModifiedDataHash(PlayStyle.SINGLE));
		assertNull(cc.getModifiedDateTime(PlayStyle.DOUBLE));
		assertNull(cc.getModifiedDataHash(PlayStyle.DOUBLE));
		assertEquals(0, cc.getCount());
	}

	// ContentCollection(TableDescription, ZonedDateTime, ZonedDateTime, String, ZonedDateTime, String, Collection<ContentDescription>)
	// NullPointerException tableDesc が null
	@Test
	public void testContentCollection_NullTableDesc() throws Exception {
		var ex = NullPointerException.class;
		assertThrows(ex, () -> new ContentCollection(null, ZDT, ZDT, HASH, ZDT, HASH, List.of()));
	}

	// ContentCollection(TableDescription, ZonedDateTime, ZonedDateTime, String, ZonedDateTime, String, Collection<ContentDescription>)
	// NullPointerException lastUpdateDateTime が null
	@Test
	public void testContentCollection_NullLastUpdateDateTime() throws Exception {
		var ex = NullPointerException.class;
		assertThrows(ex, () -> new ContentCollection(TD, null, ZDT, HASH, ZDT, HASH, List.of()));
	}

	// ContentCollection(TableDescription, ZonedDateTime, ZonedDateTime, String, ZonedDateTime, String, Collection<ContentDescription>)
	// NullPointerException contents が null
	@Test
	public void testContentCollection_NullContents() throws Exception {
		var ex = NullPointerException.class;
		assertThrows(ex, () -> new ContentCollection(TD, ZDT, ZDT, HASH, ZDT, HASH, null));
	}

	// ContentCollection(TableDescription, ZonedDateTime, ZonedDateTime, String, ZonedDateTime, String, Collection<ContentDescription>)
	// IllegalArgumentException spModifiedDataHash が文字列のSHA-256の形式ではない
	@Test
	public void testContentCollection_InvalidSpDataHash() throws Exception {
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> new ContentCollection(TD, ZDT, ZDT, "???", ZDT, HASH, List.of()));
	}

	// ContentCollection(TableDescription, ZonedDateTime, ZonedDateTime, String, ZonedDateTime, String, Collection<ContentDescription>)
	// IllegalArgumentException dpModifiedDataHash が文字列のSHA-256の形式ではない
	@Test
	public void testContentCollection_InvalidDpDataHash() throws Exception {
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> new ContentCollection(TD, ZDT, ZDT, HASH, ZDT, "???", List.of()));
	}

	// getTableDescription()
	// 正しい値を返すこと
	@Test
	public void testGetTableDescription() {
		// Do nothing: コンストラクタで確認済み
	}

	// getLastUpdateDateTime()
	// 正しい値を返すこと
	@Test
	public void testGetLastUpdateDateTime() {
		// Do nothing: コンストラクタで確認済み
	}

	// getModifiedDateTime(PlayStyle)
	// 正しい値を返すこと
	@Test
	public void testGetModifiedDateTime_Normal() {
		// Do nothing: コンストラクタで確認済み
	}

	// getModifiedDateTime(PlayStyle)
	// NullPointerException playStyle が null
	@Test
	public void testGetModifiedDateTime_NullPlayStyle() {
		var ex = NullPointerException.class;
		var cc = new ContentCollection(TD, ZDT, ZDT, HASH, ZDT, HASH, List.of());
		assertThrows(ex, () -> cc.getModifiedDateTime(null));
	}

	// getModifiedDataHash(PlayStyle)
	// 正しい値を返すこと
	@Test
	public void testGetModifiedDataHash_Normal() {
		// Do nothing: コンストラクタで確認済み
	}

	// getModifiedDataHash(PlayStyle)
	// NullPointerException playStyle が null
	@Test
	public void testGetModifiedDataHash_NullPlayStyle() {
		var ex = NullPointerException.class;
		var cc = new ContentCollection(TD, ZDT, ZDT, HASH, ZDT, HASH, List.of());
		assertThrows(ex, () -> cc.getModifiedDataHash(null));
	}

	// getCount()
	// 正しい値を返すこと
	@Test
	public void testGetCount() {
		// Do nothing: コンストラクタで確認済み
	}


	// all()
	// オブジェクト生成時の順で走査されること
	@Test
	public void testAll() {
		var cd1 = new ContentDescription("t1", "a1", PlayStyle.SINGLE, 0, null, null, null, null);
		var cd2 = new ContentDescription("t2", "a2", PlayStyle.SINGLE, 0, null, null, null, null);
		var cd3 = new ContentDescription("t3", "a3", PlayStyle.SINGLE, 0, null, null, null, null);
		var cd4 = new ContentDescription("t4", "a4", PlayStyle.SINGLE, 0, null, null, null, null);
		var contents = List.of(cd1, cd2, cd3, cd4);
		var cc = new ContentCollection(TD, ZDT, ZDT, HASH, ZDT, HASH, contents);
		var list = cc.all().collect(Collectors.toList());
		assertEquals(4, list.size());
		assertSame(cd1, list.get(0));
		assertSame(cd2, list.get(1));
		assertSame(cd3, list.get(2));
		assertSame(cd4, list.get(3));
	}

	// get(int)
	// 正しい値を返すこと
	@Test
	public void testGet_Normal() {
		// Do nothing: コンストラクタで確認済み
	}

	// get(int)
	// IndexOutOfBoundsException index が0未満または件数以上
	@Test
	public void testGet_OutOfIndex() {
		var cd1 = new ContentDescription("t1", "a1", PlayStyle.SINGLE, 0, null, null, null, null);
		var cc = new ContentCollection(TD, ZDT, ZDT, HASH, ZDT, HASH, List.of(cd1));
		var ex = IndexOutOfBoundsException.class;
		assertThrows(ex, () -> cc.get(-1));
		assertThrows(ex, () -> cc.get(cc.getCount()));
	}

	// query(String, String, String, String)
	// タイトル、アーティスト、プレースタイル、MD5が違っていてもSHA-256が一致している場合、その楽曲情報を返すこと
	@Test
	public void testQuery_MatchSha256() throws Exception {
		var cc = testQuery_TestData();
		var cd = cc.query("?", "?", PlayStyle.SINGLE, MD5_1, SHA256_2);
		assertNotNull(cd);
		assertSame("t2", cd.getTitle());
	}

	// query(String, String, String, String)
	// タイトル、アーティスト、プレースタイルが違っていてもMD5が一致している場合、その楽曲情報を返すこと
	@Test
	public void testQuery_MatchMd5() throws Exception {
		var cc = testQuery_TestData();
		var cd = cc.query("t1", "a1", PlayStyle.SINGLE, MD5_3, SHA256_X);
		assertNotNull(cd);
		assertSame("t3", cd.getTitle());
	}

	// query(String, String, String, String)
	// タイトル、アーティスト、プレースタイルが一致している場合、その楽曲情報を返すこと
	@Test
	public void testQuery_MatchMeta() throws Exception {
		var cc = testQuery_TestData();
		var cd = cc.query("t1", "a1", PlayStyle.SINGLE, null, null);
		assertNotNull(cd);
		assertSame("t1", cd.getTitle());
	}

	// query(String, String, String, String)
	// タイトル一致、アーティスト一致、プレースタイル不一致の場合、nullを返すこと
	@Test
	public void testQuery_UnmatchPlayStyle() throws Exception {
		var cc = testQuery_TestData();
		var cd = cc.query("t1", "a1", PlayStyle.DOUBLE, null, null);
		assertNull(cd);
	}

	// query(String, String, String, String)
	// タイトル一致、アーティスト不一致、プレースタイル一致の場合nullを返すこと
	@Test
	public void testQuery_UnmatchArtist() throws Exception {
		var cc = testQuery_TestData();
		var cd = cc.query("t1", "ax", PlayStyle.SINGLE, null, null);
		assertNull(cd);
	}

	// query(String, String, String, String)
	// タイトル不一致、アーティスト一致、プレースタイル一致の場合、nullを返すこと
	@Test
	public void testQuery_UnmatchTitle() throws Exception {
		var cc = testQuery_TestData();
		var cd = cc.query("tx", "a1", PlayStyle.SINGLE, null, null);
		assertNull(cd);
	}

	// query(String, String, String, String)
	// タイトル、アーティスト、プレースタイルが全て不一致の場合、nullを返すこと
	@Test
	public void testQuery_UnmatchMeta() throws Exception {
		var cc = testQuery_TestData();
		var cd = cc.query("tx", "ax", PlayStyle.DOUBLE, null, null);
		assertNull(cd);
	}

	// query(String, String, String, String)
	// NullPointerException title が null
	@Test
	public void testQuery_NullTitle() throws Exception {
		var cc = testQuery_TestData();
		assertThrows(NullPointerException.class, () -> cc.query(null, "a1", PlayStyle.SINGLE, MD5_1, SHA256_1));
	}

	// query(String, String, String, String)
	// NullPointerException artist が null
	@Test
	public void testQuery_NullArtist() throws Exception {
		var cc = testQuery_TestData();
		assertThrows(NullPointerException.class, () -> cc.query("t1", null, PlayStyle.SINGLE, MD5_1, SHA256_1));
	}

	// query(String, String, String, String)
	// NullPointerException playStyle が null
	@Test
	public void testQuery_NullPlayStyle() throws Exception {
		var cc = testQuery_TestData();
		assertThrows(NullPointerException.class, () -> cc.query("t1", "a1", null, MD5_1, SHA256_1));
	}

	private static ContentCollection testQuery_TestData() {
		var cd1 = new ContentDescription("t1", "a1", PlayStyle.SINGLE, 0, null, null, MD5_1, SHA256_1);
		var cd2 = new ContentDescription("t2", "a2", PlayStyle.SINGLE, 0, null, null, MD5_2, SHA256_2);
		var cd3 = new ContentDescription("t3", "a3", PlayStyle.SINGLE, 0, null, null, MD5_3, SHA256_3);
		var contents = List.of(cd1, cd2, cd3);
		return new ContentCollection(TD, ZDT, ZDT, HASH, ZDT, HASH, contents);
	}
}
