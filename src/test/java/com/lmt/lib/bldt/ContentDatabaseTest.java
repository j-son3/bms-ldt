package com.lmt.lib.bldt;

import static java.util.Objects.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLSession;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.lmt.lib.bldt.internal.LockFile;

public class ContentDatabaseTest {
	private static String ID_UPDATE1 = "update1";
	private static String ID_UPDATE2 = "update2";

	private static Path sDefaultLocation;
	private static Path sTmpDir;

	private Map<String, TableDescription> mOrgTableDescs;

	@FunctionalInterface
	private interface UpdateSender {
		HttpResponse<InputStream> send(HttpRequest request) throws IOException, InterruptedException;
	}

	private static class UpdateDatabase extends ContentDatabase {
		private UpdateSender mSender;

		UpdateDatabase(Path path, UpdateSender sender) throws IOException {
			super(path, true);
			mSender = sender;
		}

		@Override
		HttpResponse<InputStream> send(HttpClient client, HttpRequest request)
				throws IOException, InterruptedException {
			return mSender.send(request);
		}
	}

	private static class UpdateResponse implements HttpResponse<InputStream> {
		private int mStatusCode;
		private Map<String, List<String>> mHeaders;
		private byte[] mRaw;
		UpdateResponse(int status, Map<String, List<String>> headers) { this(status, headers, new byte[] {}); }
		UpdateResponse(int status, Map<String, List<String>> headers, byte[] raw) {
			mStatusCode = status;
			mHeaders = headers;
			mRaw = raw;
		}
		@Override public int statusCode() { return mStatusCode; }
		@Override public HttpRequest request() { return null; }
		@Override public Optional<HttpResponse<InputStream>> previousResponse() { return Optional.empty(); }
		@Override public HttpHeaders headers() { return HttpHeaders.of(mHeaders, (a, b) -> true); }
		@Override public InputStream body() { return new ByteArrayInputStream(mRaw); }
		@Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
		@Override public URI uri() { return URI.create("http://example.com"); }
		@Override public Version version() { return Version.HTTP_1_1; }
	}

	@BeforeAll
	public static void beforeAll() throws Exception {
		sDefaultLocation = ContentDatabase.DEFAULT_LOCATION;
		sTmpDir = Tests.mktmpdir(ContentDatabaseTest.class);
	}

	@AfterAll
	public static void afterAll() throws Exception {
		Tests.rmtmpdir(ContentDatabaseTest.class);
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		mOrgTableDescs = new LinkedHashMap<>(Tests.getsf(DifficultyTables.class, "sTableDescs"));
	}

	@AfterEach
	public void afterEach() throws Exception {
		// テストのために一時的に変更した値を元に戻す
		ContentDatabase.DEFAULT_LOCATION = sDefaultLocation;
		Tests.setsf(DifficultyTables.class, "sTableDescs", mOrgTableDescs);

		// フォルダ内の全ファイル・フォルダを削除する
		Files.walk(sTmpDir)
				.filter(p -> !p.equals(sTmpDir))
				.sorted(Comparator.reverseOrder())
				.forEach(p -> p.toFile().delete());
	}

	// ContentDatabase() ※詳細なテストは ContentDatabase(Path, boolean) で実施する
	// デフォルトの格納先パスが存在しない場合はフォルダを作成し、空のデータベースとなること
	@Test
	public void testContentDatabase1_LocationNotExist() throws Exception {
		ContentDatabase.DEFAULT_LOCATION = sTmpDir.resolve("new_folder");
		assertEmptyDatabase(new ContentDatabase());
	}

	// ContentDatabase()
	// デフォルトの格納先パスが存在し、フォルダの中身が空の場合は空のデータベースとなること
	@Test
	public void testContentDatabase1_LocationExistAndEmpty() throws Exception {
		var newFolder = sTmpDir.resolve("new_folder");
		ContentDatabase.DEFAULT_LOCATION = newFolder;
		Files.createDirectories(newFolder);
		assertEmptyDatabase(new ContentDatabase());
	}

	// ContentDatabase()
	// デフォルトの格納先パスが存在し、既存データベースが存在する場合は当該データベースが読み込まれること
	@Test
	public void testContentDatabase1_LocationExist() throws Exception {
		ContentDatabase.DEFAULT_LOCATION = setupCommonTestData();
		var db = new ContentDatabase();
		assertCommonTestData(db);
	}

	// ContentDatabase(Path, boolean)
	// 指定の格納先パスが存在し、作成フラグがfalseの場合、そのパスからの読み込みが行われること
	@Test
	public void testContentDatabase2_LocationExist_NotCreate() throws Exception {
		var db = new ContentDatabase(setupCommonTestData(), false);
		assertCommonTestData(db);
	}

	// ContentDatabase(Path, boolean)
	// 指定の格納先パスが存在し、作成フラグがtrueの場合、そのパスからの読み込みが行われること
	@Test
	public void testContentDatabase2_LocationExist_Create() throws Exception {
		var db = new ContentDatabase(setupCommonTestData(), true);
		assertCommonTestData(db);
	}

	// ContentDatabase(Path, boolean)
	// 指定の格納先パスがファイルの場合、NoSuchFileExceptionがスローされること
	@Test
	public void testContentDatabase2_LocationIsFile() throws Exception {
		var path = sTmpDir.resolve("file");
		Files.createFile(path);
		assertThrows(NoSuchFileException.class, () -> new ContentDatabase(path, false));
	}

	// ContentDatabase(Path, boolean)
	// 指定の格納先パスが存在せず、作成フラグがfalseの場合、NoSuchFileExceptionがスローされること
	@Test
	public void testContentDatabase2_LocationNotExist_NotCreate() throws Exception {
		var path = sTmpDir.resolve("not_found");
		assertThrows(NoSuchFileException.class, () -> new ContentDatabase(path, false));
	}

	// ContentDatabase(Path, boolean)
	// 指定の格納先パスが存在せず、作成フラグがtrueの場合、空のデータベースとなること
	@Test
	public void testContentDatabase2_LocationNotExist_Create() throws Exception {
		var path = sTmpDir.resolve("new_folder");
		assertEmptyDatabase(new ContentDatabase(path, true));
	}

	// ContentDatabase(Path, boolean)
	// 格納先パスが書き込み中の場合、IllegalStateExceptionがスローされること
	@Test
	public void testContentDatabase2_WritingLocationByOther() throws Exception {
		var path = sTmpDir.resolve("writing");
		var writeLockF = path.resolve(writeLockFileName());
		var writeLock = new LockFile(writeLockF);
		Files.createDirectories(path);
		assertTrue(writeLock.lock());
		assertThrows(IllegalStateException.class, () -> new ContentDatabase(path, false));
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報FのJSONが構文エラーの場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_FileParseError() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのversionが未存在の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_VersionNotExist() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのversionが想定と異なる値の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_VersionInvalid() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのidが未存在の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_IdNotExist() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのidと難易度表のIDが不一致の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_IdUnmatch() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報FのlastUpdatedが未存在の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_LastUpdatedNotExist() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報FのlastUpdatedが不正な記述の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_LastUpdatedInvalid() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodifiedが未存在の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ModifiedNotExist() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodifiedが配列以外の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ModifiedNotArray() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodifiedの項目数が2個以外の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ModifiedItemNot2() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodifiedの項目がオブジェクト以外の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ModifiedItemNotObject() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodified.dateTimeが未存在の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ModifiedDateTimeNotExist() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodified.dateTimeがnullの場合、最終更新日時がnullになること
	@Test
	public void testContentDatabase2_ModifiedDateTimeNull() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.getModifiedDateTime(PlayStyle.SINGLE));
			assertNull(cc.getModifiedDateTime(PlayStyle.DOUBLE));
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodified.dateTimeが不正な記述の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ModifiedDateTimeInvalid() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodified.dateTimeが正しい日時の場合、最終更新日時が正しく設定されること
	@Test
	public void testContentDatabase2_ModifiedDateTimeValid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals("2025-02-03T04:05:06Z", cc.getModifiedDateTime(PlayStyle.SINGLE).toString());
			assertEquals("2023-04-05T06:07:08+09:00[Asia/Tokyo]", cc.getModifiedDateTime(PlayStyle.DOUBLE).toString());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodified.dataHashが未存在の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ModifiedDataHashNotExist() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodified.dataHashがnullの場合、最終更新データハッシュがnullになること
	@Test
	public void testContentDatabase2_ModifiedDataHashNull() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.getModifiedDataHash(PlayStyle.SINGLE));
			assertNull(cc.getModifiedDataHash(PlayStyle.DOUBLE));
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodified.dataHashが不正な記述の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ModifiedDataHashInvalid() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのmodified.dataHashが正しい記述の場合、最終更新データハッシュが正しく設定されること
	@Test
	public void testContentDatabase2_ModifiedDataHashValid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals("1111111111111111111111111111111111111111111111111111111111111111",
					cc.getModifiedDataHash(PlayStyle.SINGLE));
			assertEquals("2222222222222222222222222222222222222222222222222222222222222222",
					cc.getModifiedDataHash(PlayStyle.DOUBLE));
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontentsが未存在の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ContentsNotExist() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontentsが配列以外の場合、IOExceptionがスローされること
	@Test
	public void testContentDatabase2_ContentsNotArray() throws Exception {
		testContentDatabase2_ThrowIOException();
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontentsが0件の場合、楽曲情報リストが空になること
	@Test
	public void testContentDatabase2_ContentsItem0() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(0, cc.getCount());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontentsの項目がオブジェクト以外の場合、当該データは無視されること
	@Test
	public void testContentDatabase2_ContentsItemNotObject() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(2, cc.getCount());
			assertEquals("A1", cc.get(0).getTitle());
			assertEquals("A2", cc.get(1).getTitle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.titleが未存在の場合、当該楽曲情報は無視されること
	@Test
	public void testContentDatabase2_ContentsTitleNotExist() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(1, cc.getCount());
			assertEquals("A1", cc.get(0).getTitle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.titleが空の場合、当該楽曲情報は無視されること
	@Test
	public void testContentDatabase2_ContentsTitleEmpty() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(1, cc.getCount());
			assertEquals("A1", cc.get(0).getTitle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.artistが未存在の場合、アーティストは空となること
	@Test
	public void testContentDatabase2_ContentsArtistNotExist() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(1, cc.getCount());
			assertEquals("A1", cc.get(0).getTitle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.dpModeが未存在の場合、プレースタイルはSINGLEになること
	@Test
	public void testContentDatabase2_ContentsDpModeNotExist() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(PlayStyle.SINGLE, cc.get(0).getPlayStyle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.dpModeがブール値以外の場合、プレースタイルはSINGLEになること
	@Test
	public void testContentDatabase2_ContentsDpModeNotBoolean() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(PlayStyle.SINGLE, cc.get(0).getPlayStyle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.levelIndexが未存在の場合、当該楽曲情報は無視されること
	@Test
	public void testContentDatabase2_ContentsLevelIndexNotExist() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(1, cc.getCount());
			assertEquals("A1", cc.get(0).getTitle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.levelIndexが整数以外の場合、当該楽曲情報は無視されること
	@Test
	public void testContentDatabase2_ContentsLevelIndexNotInt() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(1, cc.getCount());
			assertEquals("A1", cc.get(0).getTitle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.levelIndexが負の値の場合、当該楽曲情報は無視されること
	@Test
	public void testContentDatabase2_ContentsLevelIndexNegative() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(1, cc.getCount());
			assertEquals("A1", cc.get(0).getTitle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.levelIndexが当該難易度表の難易度ラベル数以上の場合、当該楽曲情報は無視されること
	@Test
	public void testContentDatabase2_ContentsLevelIndexOverflow() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals(1, cc.getCount());
			assertEquals("A1", cc.get(0).getTitle());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.bodyUrlが未存在の場合、楽曲本体入手先URLがnullになること
	@Test
	public void testContentDatabase2_ContentsBodyUrlNotExist() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.get(0).getBodyUrl());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.bodyUrlが不正な記述の場合、楽曲本体入手先URLがnullになること
	@Test
	public void testContentDatabase2_ContentsBodyUrlInvalid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.get(0).getBodyUrl());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.bodyUrlが正しい記述の場合、楽曲本体入手先URLが正しく設定されること
	@Test
	public void testContentDatabase2_ContentsBodyUrlValid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals("http://example.com/ok.zip", cc.get(0).getBodyUrl().toString());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.additionalUrlが未存在の場合、差分譜面入手先URLがnullになること
	@Test
	public void testContentDatabase2_ContentsAdditionalUrlNotExist() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.get(0).getAdditionalUrl());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.additionalUrlが不正な記述の場合、差分譜面入手先URLがnullになること
	@Test
	public void testContentDatabase2_ContentsAdditionalUrlInvalid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.get(0).getAdditionalUrl());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.additionalUrlが正しい記述の場合、差分譜面入手先URLが正しく設定されること
	@Test
	public void testContentDatabase2_ContentsAdditionalUrlValid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals("http://example.com/ok.zip", cc.get(0).getAdditionalUrl().toString());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.md5が未存在の場合、MD5がnullになること
	@Test
	public void testContentDatabase2_ContentsMd5NotExist() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.get(0).getMd5());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.md5が不正な記述の場合、MD5がnullになること
	@Test
	public void testContentDatabase2_ContentsMd5Invalid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.get(0).getMd5());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.md5が正しい記述の場合、MD5が正しく設定されること
	@Test
	public void testContentDatabase2_ContentsMd5Valid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals("aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbb", cc.get(0).getMd5());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.sha256が未存在の場合、SHA-256がnullになること
	@Test
	public void testContentDatabase2_ContentsSha256NotExist() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.get(0).getSha256());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.sha256が不正な記述の場合、SHA-256がnullになること
	@Test
	public void testContentDatabase2_ContentsSha256Invalid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertNull(cc.get(0).getSha256());
		});
	}

	// ContentDatabase(Path, boolean)
	// 難易度表情報Fのcontents.sha256が正しい記述の場合、SHA-256が正しく設定されること
	@Test
	public void testContentDatabase2_ContentsSha256Valid() throws Exception {
		testContentDatabase2_SuccessAssertion(cc -> {
			assertEquals("aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbccccccccccccccccdddddddddddddddd", cc.get(0).getSha256());
		});
	}

	// ContentDatabase(Path, boolean)
	// ユーザー登録された難易度表も読み込み対象となること
	@Test
	public void testContentDatabase2_AddedTableDescription() throws Exception {
		var url = new URL("http://example.com");
		var dp = new PlayStyleDescription("*", url, List.of("0", "Z"));
		var td = new TableDescription("org", "ORG", url, TableDescriptionTest.EMPTY_PARSER, null, dp);
		DifficultyTables.add(td);
		var path = setupTestData();
		var db = new ContentDatabase(path, false);
		var preset = db.get(Presets.SATELLITE.getId());
		assertEquals(1, preset.getCount());
		var org = db.get("org");
		assertNotNull(org);
		assertEquals("org", org.getTableDescription().getId());
		assertEquals("2025-12-31T23:59:59.999+09:00[Asia/Tokyo]", org.getLastUpdateDateTime().toString());
		assertEquals("1999-01-02T03:04:05Z", org.getModifiedDateTime(PlayStyle.SINGLE).toString());
		assertEquals("aaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbccccccccccccccccdddddddddddddddd",
				org.getModifiedDataHash(PlayStyle.SINGLE));
		assertEquals("2000-05-04T03:02:01Z", org.getModifiedDateTime(PlayStyle.DOUBLE).toString());
		assertEquals("ffffffffffffffffeeeeeeeeeeeeeeeeddddddddddddddddcccccccccccccccc",
				org.getModifiedDataHash(PlayStyle.DOUBLE));
		assertEquals(1, org.getCount());
		var c = org.get(0);
		assertEquals("ORG-TITLE", c.getTitle());
		assertEquals("ORG-ARTIST", c.getArtist());
		assertEquals(PlayStyle.DOUBLE, c.getPlayStyle());
		assertEquals("Z", dp.getLabels().get(c.getLevelIndex()));
		assertEquals("http://example.com/org-body.zip", c.getBodyUrl().toString());
		assertEquals("http://example.com/org-add.zip", c.getAdditionalUrl().toString());
		assertEquals("1111111111111111ffffffffffffffff", c.getMd5());
		assertEquals("ffffffffffffffffffffffffffffffff22222222222222222222222222222222", c.getSha256());
	}

	// ContentDatabase(Path, boolean)
	// 難易度表データベースの読み込み完了後、当該格納先パスの読み書きロックが全て解除されること
	@Test
	public void testContentDatabase2_UnlockAfterLoad() throws Exception {
		var path = setupCommonTestData();
		var readLock = new LockFile(path.resolve(readLockFileName()));
		var writeLock = new LockFile(path.resolve(writeLockFileName()));
		new ContentDatabase(path, true);
		assertTrue(readLock.test());
		assertTrue(writeLock.test());
	}

	private void testContentDatabase2_SuccessAssertion(Consumer<ContentCollection> asserter) throws Exception {
		var path = setupTestData(1);
		var db = new ContentDatabase(path, false);
		var cc = db.get(Presets.SATELLITE.getId());
		asserter.accept(cc);
	}

	private void testContentDatabase2_ThrowIOException() throws Exception {
		// 難易度表情報ファイルの内容が正しくない時にIOExceptionをスローすることが期待値のテスト
		var path = setupTestData(1);
		assertThrows(IOException.class, () -> new ContentDatabase(path, false));
	}

	// getLocation()
	// 正しい値を返すこと
	@Test
	public void testGetLocation() throws Exception {
		var path = setupCommonTestData();
		var db = new ContentDatabase(path, false);
		assertEquals(path, db.getLocation());
	}

	// all()
	// 難易度表定義の登録順で走査されること
	@Test
	public void testAll() throws Exception {
		var url = new URL("http://example.com");
		var sp = new PlayStyleDescription("*", url, List.of("0"));
		var td = new TableDescription("org", "ORG", url, TableDescriptionTest.EMPTY_PARSER, sp, null);
		DifficultyTables.add(td);
		var db = new ContentDatabase(setupCommonTestData(), false);
		var all = db.all().collect(Collectors.toList());
		var tds = DifficultyTables.all().collect(Collectors.toList());
		assertEquals(tds.size(), all.size());
		for (var i = 0; i < tds.size(); i++) {
			assertEquals(tds.get(i).getId(), all.get(i).getTableDescription().getId());
		}
	}

	// get(String)
	// 正しい値を返すこと
	@Test
	public void testGet_Normal() throws Exception {
		var db = new ContentDatabase(setupCommonTestData(), false);
		for (var preset : Presets.values()) {
			var cc = db.get(preset.getId());
			assertNotNull(cc);
			assertEquals(preset.getId(), cc.getTableDescription().getId());
		}
	}

	// get(String)
	// 該当IDの難易度表定義が存在しない場合nullを返すこと
	@Test
	public void testGet_NotFound() throws Exception {
		var db = new ContentDatabase(setupCommonTestData(), false);
		assertNull(db.get("not_found"));
	}

	// get(String)
	// NullPointerException id が null
	@Test
	public void testGet_NullId() throws Exception {
		var db = new ContentDatabase(setupCommonTestData(), false);
		assertThrows(NullPointerException.class, () -> db.get(null));
	}

	// update(HttpClient, String, Duration, UpdateProgress) ※詳細なテストは全難易度表更新版メソッドで実施する
	// 指定したIDに該当する難易度表定義のみ更新が実行されること
	@Test
	public void testUpdate1_Normal() throws Exception {

	}

	// update(HttpClient, String, Duration, UpdateProgress)
	// 更新が行われなかった難易度表では、全ての情報が更新前の状態となること
	@Test
	public void testUpdate1_DontSave() throws Exception {
		var db = setupUpdateDatabase((td, ps, raw) -> {
			return List.of(new ContentDescription("X", "Y", PlayStyle.DOUBLE, 1, null, null, null, null));
		});
		db.update(httpClient(), ID_UPDATE2, null, UpdateProgress.nop());
		var cc1 = db.get(ID_UPDATE1);
		var cc2 = db.get(ID_UPDATE2);
		assertEquals(2, cc1.getCount());
		assertEquals(1, cc2.getCount());
		var cd = cc2.get(0);
		assertEquals("X", cd.getTitle());
		assertEquals("Y", cd.getArtist());
		assertEquals(PlayStyle.DOUBLE, cd.getPlayStyle());
		assertEquals(1, cd.getLevelIndex());
	}

	// update(HttpClient, String, Duration, UpdateProgress)
	// NullPointerException client が null
	@Test
	public void testUpdate1_NullClient() throws Exception {
		var db = setupEmptyDatabase();
		var ex = NullPointerException.class;
		assertThrows(ex, () -> db.update(null, Presets.STELLA.getId(), Duration.ofSeconds(1), UpdateProgress.nop()));
	}

	// update(HttpClient, String, Duration, UpdateProgress)
	// NullPointerException id が null
	@Test
	public void testUpdate1_NullId() throws Exception {
		var db = setupEmptyDatabase();
		var ex = NullPointerException.class;
		assertThrows(ex, () -> db.update(httpClient(), null, Duration.ofSeconds(1), UpdateProgress.nop()));
	}

	// update(HttpClient, String, Duration, UpdateProgress)
	// NullPointerException progress が null
	@Test
	public void testUpdate1_NullProgress() throws Exception {
		var db = setupEmptyDatabase();
		var ex = NullPointerException.class;
		assertThrows(ex, () -> db.update(httpClient(), Presets.STELLA.getId(), Duration.ofSeconds(1), null));
	}

	// update(HttpClient, String, Duration, UpdateProgress)
	// IllegalArgumentException id に該当する難易度表定義が存在しない
	@Test
	public void testUpdate1_IdNotFound() throws Exception {
		var db = setupEmptyDatabase();
		var ex = IllegalArgumentException.class;
		assertThrows(ex, () -> db.update(httpClient(), "not_found", Duration.ofSeconds(1), UpdateProgress.nop()));
	}

	// update(HttpClient, String, Duration, UpdateProgress)
	// InterruptedException スレッド割り込みによる更新処理の中止が発生した
	@Test
	@Timeout(1000)
	public void testUpdate1_Interrupted() throws Exception {
		var db = setupUpdateDatabase(r -> { Thread.sleep(Long.MAX_VALUE); return null; });
		var ex = InterruptedException.class;
		Thread.currentThread().interrupt();
		assertThrows(ex, () -> db.update(httpClient(), ID_UPDATE1, null, UpdateProgress.nop()));
	}

	// update(HttpClient, String, Duration, UpdateProgress)
	// IllegalStateException 読み込み排他処理エラーが発生した
	@Test
	public void testUpdate1_ReadExclusiveError() throws Exception {
		var db = setupUpdateDatabase();
		var lockF = new LockFile(db.getLocation().resolve(readLockFileName()));
		var ex = IllegalStateException.class;
		try {
			assertTrue(lockF.lock());
			assertThrows(ex, () -> db.update(httpClient(), ID_UPDATE1, null, UpdateProgress.nop()));
		} finally {
			lockF.unlock();
		}
	}

	// update(HttpClient, String, Duration, UpdateProgress)
	// IllegalStateException 書き込み排他処理エラーが発生した
	@Test
	public void testUpdate1_WriteExclusiveError() throws Exception {
		var db = setupUpdateDatabase();
		var lockF = new LockFile(db.getLocation().resolve(writeLockFileName()));
		var ex = IllegalStateException.class;
		try {
			assertTrue(lockF.lock());
			assertThrows(ex, () -> db.update(httpClient(), ID_UPDATE1, null, UpdateProgress.nop()));
		} finally {
			lockF.unlock();
		}
	}

	// update(HttpClient, Duration, UpdateProgress)
	// clientがnullの場合、NullPointerExceptionがスローされること
	@Test
	public void testUpdate2_NullClient() throws Exception {
		var db = setupEmptyDatabase();
		var ex = NullPointerException.class;
		assertThrows(ex, () -> db.update(null, Duration.ofSeconds(1), UpdateProgress.nop()));
	}

	// update(HttpClient, Duration, UpdateProgress)
	// progressがnullの場合、NullPointerExceptionがスローされること
	@Test
	public void testUpdate2_NullProgress() throws Exception {
		var db = setupEmptyDatabase();
		var ex = NullPointerException.class;
		assertThrows(ex, () -> db.update(httpClient(), Duration.ofSeconds(1), null));
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 格納先パスが読み込み中の場合、IllegalStateExceptionがスローされること
	@Test
	public void testUpdate2_ReadingLocationByOther() throws Exception {
		var db = setupEmptyDatabase();
		var lockF = new LockFile(db.getLocation().resolve(readLockFileName()));
		var ex = IllegalStateException.class;
		try {
			assertTrue(lockF.lock());
			assertThrows(ex, () -> db.update(httpClient(), null, UpdateProgress.nop()));
		} finally {
			lockF.unlock();
		}
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 格納先パスが書き込み中の場合、IllegalStateExceptionがスローされること
	@Test
	public void testUpdate2_WritingLocationByOther() throws Exception {
		var db = setupEmptyDatabase();
		var lockF = new LockFile(db.getLocation().resolve(writeLockFileName()));
		var ex = IllegalStateException.class;
		try {
			assertTrue(lockF.lock());
			assertThrows(ex, () -> db.update(httpClient(), null, UpdateProgress.nop()));
		} finally {
			lockF.unlock();
		}
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 非対応のプレースタイルは更新対象外となること
	@Test
	public void testUpdate2_UnsupportedPlayStyle() throws Exception {
		var ids = new ArrayList<>(List.of(ID_UPDATE1, ID_UPDATE2));
		setupUpdateDatabase((td, ps, raw) -> {
			assertNotNull(td.getPlayStyleDescription(ps));
			ids.remove(td.getId());
			return List.of();
		}).update(httpClient(), null, UpdateProgress.nop());
		assertTrue(ids.isEmpty());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 難易度表情報ファイルからの読み込みが行われていない難易度表は更新が実行されること
	@Test
	public void testUpdate2_UpdateNotLoaded() throws Exception {
		var db1 = setupUpdateDatabase((td, ps, raw) -> {
			return List.of(new ContentDescription("A", "B", ps, 0, null, null, null, null));
		});
		var loc = db1.getLocation();
		db1.update(httpClient(), null, UpdateProgress.nop());
		var db2 = new ContentDatabase(loc, false);
		var c1 = db2.get(ID_UPDATE1).get(0);
		var c2 = db2.get(ID_UPDATE2).get(0);
		assertEquals("A", c1.getTitle());
		assertEquals(PlayStyle.SINGLE, c1.getPlayStyle());
		assertEquals("A", c2.getTitle());
		assertEquals(PlayStyle.DOUBLE, c2.getPlayStyle());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 楽曲情報URLがURIに変換できない場合、進捗報告でERRORが通知され、IOExceptionがスローされること
	@Test
	public void testUpdate2_ContentUrlError() throws Exception {
		setupUpdateTableDescriptions("http://example.com/err or", null, null);
		var db = setupEmptyDatabase();
		var called = new AtomicBoolean(false);
		var ex = IOException.class;
		assertThrows(ex, () -> db.update(httpClient(), null, (td, ps, i, num, sts) -> {
			if (sts == UpdateProgress.Status.ERROR) {
				assertEquals(ID_UPDATE1, td.getId());
				assertEquals(PlayStyle.SINGLE, ps);
				called.set(true);
			}
		}));
		assertTrue(called.get());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// timeoutが指定されている場合、その値が応答タイムアウトに設定されること
	@Test
	public void testUpdate2_SetResponseTimeout() throws Exception {
		var timeout = Duration.ofSeconds(3);
		var db = setupUpdateDatabase(r -> {
			assertEquals(timeout, r.timeout().get());
			return new UpdateResponse(200, Map.of());
		});
		db.update(httpClient(), timeout, UpdateProgress.nop());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 難易度情報ファイルからの読み込みが行われていない難易度表ではIf-Modified-Sinceヘッダが未設定であること
	@Test
	public void testUpdate2_IfModifiedSince_NotLoaded() throws Exception {
		var db = setupUpdateDatabase(r -> {
			assertTrue(r.headers().firstValue("If-Modified-Since").isEmpty());
			return new UpdateResponse(200, Map.of());
		});
		db.update(httpClient(), null, UpdateProgress.nop());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 最終更新日時がnullの難易度表ではIf-Modified-Sinceヘッダが未設定であること
	@Test
	public void testUpdate2_IfModifiedSince_ModifiedDateTimeNull() throws Exception {
		var db = setupUpdateDatabase(r -> {
			assertTrue(r.headers().firstValue("If-Modified-Since").isEmpty());
			return new UpdateResponse(200, Map.of());
		});
		db.update(httpClient(), null, UpdateProgress.nop());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 最終更新日時が設定されている難易度表ではIf-Modified-Sinceヘッダにその日時が設定されていること
	@Test
	public void testUpdate2_IfModifiedSince_ModifiedDateTimeValid() throws Exception {
		var db = setupUpdateDatabase(r -> {
			if (r.uri().toString().equals("http://example.com/1")) {
				assertEquals(
						ZonedDateTime.parse("2025-12-11T10:09:08.700Z").format(DateTimeFormatter.RFC_1123_DATE_TIME),
						r.headers().firstValue("If-Modified-Since").get());
			} else if (r.uri().toString().equals("http://example.com/2")) {
				assertEquals(
						ZonedDateTime.parse("2025-07-08T09:10:11.120Z").format(DateTimeFormatter.RFC_1123_DATE_TIME),
						r.headers().firstValue("If-Modified-Since").get());
			} else {
				fail(r.uri().toString() + ": Unexpected URL");
			}
			return new UpdateResponse(200, Map.of());
		});
		db.update(httpClient(), null, UpdateProgress.nop());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// サーバ応答が304の場合、進捗報告でUNNECESSARYが通知され、更新が行われないこと
	@Test
	public void testUpdate2_Response304() throws Exception {
		var numCall = new AtomicInteger(0);
		var db = setupUpdateDatabase(r -> new UpdateResponse(304, Map.of()));
		var mod1 = Files.getLastModifiedTime(db.getLocation().resolve(ID_UPDATE1 + ".json"));
		var mod2 = Files.getLastModifiedTime(db.getLocation().resolve(ID_UPDATE2 + ".json"));
		db.update(httpClient(), null, (td, ps, i, num, sts) -> {
			if (sts == UpdateProgress.Status.UNNECESSARY) { numCall.incrementAndGet(); }
		});
		assertEquals(2, numCall.get());
		assertEquals(mod1, Files.getLastModifiedTime(db.getLocation().resolve(ID_UPDATE1 + ".json")));
		assertEquals(mod2, Files.getLastModifiedTime(db.getLocation().resolve(ID_UPDATE2 + ".json")));
	}

	// update(HttpClient, Duration, UpdateProgress)
	// サーバ応答が200以外の場合、進捗報告でERRORが通知され、IOExceptionがスローされること
	@Test
	public void testUpdate2_ResponseNot200() throws Exception {
		var db = setupUpdateDatabase(r -> new UpdateResponse(404, Map.of()));
		var ex = IOException.class;
		assertThrows(ex, () -> db.update(httpClient(), null, UpdateProgress.nop()));
	}

	// update(HttpClient, Duration, UpdateProgress)
	// サーバ応答が200の場合、楽曲情報パーサが実行されること
	@Test
	public void testUpdate2_Response200() throws Exception {
		var callStatus = new AtomicInteger(0);
		var db = setupUpdateDatabase(
				r -> {
					assertEquals(0, callStatus.getAndSet(1));
					return new UpdateResponse(200, Map.of());
				},
				(td, ps, raw) -> {
					assertEquals(1, callStatus.getAndSet(0));
					return List.of();
				});
		db.update(httpClient(), null, UpdateProgress.nop());
		assertEquals(0, callStatus.get());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 応答にLast-Modifiedが含まれなくても処理が更新が行われること
	@Test
	public void testUpdate2_LastModified_NotFound() throws Exception {
		var db = setupUpdateDatabase(r -> new UpdateResponse(200, Map.of()));
		var now = ZonedDateTime.now();
		db.update(httpClient(), null, UpdateProgress.nop());
		var cc1 = db.get(ID_UPDATE1);
		var cc2 = db.get(ID_UPDATE2);
		assertTrue(cc1.getLastUpdateDateTime().compareTo(now) >= 0);
		assertEquals("2025-12-11T10:09:08.700Z", cc1.getModifiedDateTime(PlayStyle.SINGLE).toString());
		assertEquals(0, cc1.getCount());
		assertTrue(cc2.getLastUpdateDateTime().compareTo(now) >= 0);
		assertEquals("2025-07-08T09:10:11.120Z", cc2.getModifiedDateTime(PlayStyle.DOUBLE).toString());
		assertEquals(0, cc2.getCount());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 応答のLast-Modifiedが不正な記述の場合でも更新が行われること
	@Test
	public void testUpdate2_LastModified_Invalid() throws Exception {
		var db = setupUpdateDatabase(r -> {
			return new UpdateResponse(200, Map.of("Last-Modified", List.of("UNKNOWN")));
		});
		var now = ZonedDateTime.now();
		db.update(httpClient(), null, UpdateProgress.nop());
		var cc1 = db.get(ID_UPDATE1);
		var cc2 = db.get(ID_UPDATE2);
		assertTrue(cc1.getLastUpdateDateTime().compareTo(now) >= 0);
		assertEquals("2025-12-11T10:09:08.700Z", cc1.getModifiedDateTime(PlayStyle.SINGLE).toString());
		assertEquals(0, cc1.getCount());
		assertTrue(cc2.getLastUpdateDateTime().compareTo(now) >= 0);
		assertEquals("2025-07-08T09:10:11.120Z", cc2.getModifiedDateTime(PlayStyle.DOUBLE).toString());
		assertEquals(0, cc2.getCount());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 応答のLast-Modifiedが正しい記述の場合、modified.dateTimeにその値が設定されること
	@Test
	public void testUpdate2_LastModified_Valid() throws Exception {
		var zdt = ZonedDateTime.parse("2025-11-22T11:22:33Z");
		var db = setupUpdateDatabase(r -> {
			var lm = zdt.format(DateTimeFormatter.RFC_1123_DATE_TIME);
			return new UpdateResponse(200, Map.of("Last-Modified", List.of(lm)));
		});
		db.update(httpClient(), null, UpdateProgress.nop());
		var cc1 = db.get(ID_UPDATE1);
		var cc2 = db.get(ID_UPDATE2);
		assertEquals(zdt, cc1.getModifiedDateTime(PlayStyle.SINGLE));
		assertEquals(zdt, cc2.getModifiedDateTime(PlayStyle.DOUBLE));
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 応答データのハッシュ値が一致する場合、UNNECESSARYが通知され、更新が行われないこと
	@Test
	public void testUpdate2_DataHash_Match() throws Exception {
		var numCall = new AtomicInteger(0);
		var db = setupUpdateDatabase();
		var mod1 = Files.getLastModifiedTime(db.getLocation().resolve(ID_UPDATE1 + ".json"));
		var mod2 = Files.getLastModifiedTime(db.getLocation().resolve(ID_UPDATE2 + ".json"));
		db.update(httpClient(), null, (td, ps, i, num, sts) -> {
			if (sts == UpdateProgress.Status.UNNECESSARY) { numCall.incrementAndGet(); }
		});
		assertEquals(2, numCall.get());
		assertEquals(mod1, Files.getLastModifiedTime(db.getLocation().resolve(ID_UPDATE1 + ".json")));
		assertEquals(mod2, Files.getLastModifiedTime(db.getLocation().resolve(ID_UPDATE2 + ".json")));
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 応答データのハッシュ値が一致しない場合、UNNECESSARYが通知されず、modified.dataHashにハッシュ値が設定されること
	@Test
	public void testUpdate2_DataHash_Unmatch() throws Exception {
		var numCall = new AtomicInteger(0);
		var db = setupUpdateDatabase();
		db.update(httpClient(), null, (td, ps, i, num, sts) -> {
			if (sts == UpdateProgress.Status.UNNECESSARY) { numCall.incrementAndGet(); }
		});
		var hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
		var cc1 = db.get(ID_UPDATE1);
		var cc2 = db.get(ID_UPDATE2);
		assertEquals(0, numCall.get());
		assertEquals(hash, cc1.getModifiedDataHash(PlayStyle.SINGLE));
		assertEquals(hash, cc2.getModifiedDataHash(PlayStyle.DOUBLE));
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 楽曲情報パーサがnullを返した場合、ERRORが通知され、IOExceptionがスローされること
	@Test
	public void testUpdate2_Parser_ReturnedNull() throws Exception {
		var called = new AtomicBoolean(false);
		var db = setupUpdateDatabase((td, ps, raw) -> null);
		assertThrows(IOException.class, () -> {
			db.update(httpClient(), null, (td, ps, i, num, sts) -> {
				if (sts == UpdateProgress.Status.ERROR) { called.set(true); }
			});
		});
		assertTrue(called.get());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 楽曲情報パーサが例外をスローした場合、ERRORが通知され、IOExceptionがスローされること
	@Test
	public void testUpdate2_Parser_ThrownException() throws Exception {
		var called = new AtomicBoolean(false);
		var db = setupUpdateDatabase((td, ps, raw) -> { throw new UnsupportedOperationException(); });
		var thrown = assertThrows(IOException.class, () -> {
			db.update(httpClient(), null, (td, ps, i, num, sts) -> {
				if (sts == UpdateProgress.Status.ERROR) { called.set(true); }
			});
		});
		assertTrue(called.get());
		assertEquals(UnsupportedOperationException.class, thrown.getCause().getClass());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 難易度表更新が行われた場合の進捗報告シーケンスが想定通りであること
	@Test
	public void testUpdate2_UpdateProgressSequence() throws Exception {
		var seq = new UpdateProgress.Status[] {
				UpdateProgress.Status.START,
				UpdateProgress.Status.DONE,
				UpdateProgress.Status.START,
				UpdateProgress.Status.UNNECESSARY,
		};
		var n = new AtomicInteger(-1);
		var db = setupUpdateDatabase(r -> {
			var status = r.uri().toString().equals("http://example.com/1") ? 200 : 304;
			return new UpdateResponse(status, Map.of());
		});
		db.update(httpClient(), null, (td, ps, i, num, sts) -> {
			var si = n.incrementAndGet();
			assertEquals(seq[si], sts);
		});
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 難易度表情報の更新内容が正しい内容であること
	@Test
	public void testUpdate2_ContentFile() throws Exception {
		var bodies = new URL[] {
				new URL("http://example.com/body1-after.zip"),
				new URL("http://example.com/body2-after.zip"),
				new URL("http://example.com/body3-after.zip") };
		var adds = new URL[] {
				new URL("http://example.com/add1-after.zip"),
				new URL("http://example.com/add2-after.zip"),
				new URL("http://example.com/add3-after.zip") };
		var md5s = new String[] {
				"11111111111111111111111111111111",
				"22222222222222222222222222222222",
				"33333333333333333333333333333333" };
		var sha256s = new String[] {
				"1111111111111111111111111111111111111111111111111111111111111111",
				"2222222222222222222222222222222222222222222222222222222222222222",
				"3333333333333333333333333333333333333333333333333333333333333333" };
		var db = setupUpdateDatabase(
				r -> {
					var upd1 = r.uri().toString().equals("http://example.com/1");
					var mod = ZonedDateTime.parse(upd1 ? "2025-01-02T03:04:05Z" : "2025-05-04T03:02:01Z");
					var raw = upd1 ? new byte[] { 'a' } : new byte[] { 'b' };
					return new UpdateResponse(
							200,
							Map.of("Last-Modified", List.of(mod.format(DateTimeFormatter.RFC_1123_DATE_TIME))),
							raw);
				},
				(td, ps, raw) -> {
					var upd1 = td.getId().equals(ID_UPDATE1);
					if (upd1) {
						return List.of(
								new ContentDescription("T1-1", "A1-1", ps, 0, bodies[0], adds[0], md5s[0], sha256s[0]),
								new ContentDescription("T1-2", "A1-2", ps, 1, bodies[1], adds[1], md5s[1], sha256s[1]),
								new ContentDescription("T1-3", "A1-3", ps, 2, bodies[2], adds[2], md5s[2], sha256s[2]));
					} else {
						return List.of(
								new ContentDescription("T2-1", "A2-1", ps, 2, bodies[2], adds[2], md5s[2], sha256s[2]),
								new ContentDescription("T2-2", "A2-2", ps, 1, bodies[1], adds[1], md5s[1], sha256s[1]));
					}
				});
		var now = ZonedDateTime.now();
		db.update(httpClient(), null, UpdateProgress.nop());

		var db2 = new ContentDatabase(db.getLocation(), false);
		var cc1 = db2.get(ID_UPDATE1);
		assertTrue(cc1.getLastUpdateDateTime().compareTo(now) >= 0);
		assertEquals("2025-01-02T03:04:05Z", cc1.getModifiedDateTime(PlayStyle.SINGLE).toString());
		assertEquals("ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
				cc1.getModifiedDataHash(PlayStyle.SINGLE));
		assertNull(cc1.getModifiedDateTime(PlayStyle.DOUBLE));
		assertNull(cc1.getModifiedDataHash(PlayStyle.DOUBLE));
		assertEquals(3, cc1.getCount());
		assertEquals("T1-1", cc1.get(0).getTitle());
		assertEquals("A1-1", cc1.get(0).getArtist());
		assertEquals(PlayStyle.SINGLE, cc1.get(0).getPlayStyle());
		assertEquals(bodies[0], cc1.get(0).getBodyUrl());
		assertEquals(adds[0], cc1.get(0).getAdditionalUrl());
		assertEquals(md5s[0], cc1.get(0).getMd5());
		assertEquals(sha256s[0], cc1.get(0).getSha256());
		assertEquals("T1-2", cc1.get(1).getTitle());
		assertEquals("A1-2", cc1.get(1).getArtist());
		assertEquals(PlayStyle.SINGLE, cc1.get(1).getPlayStyle());
		assertEquals(bodies[1], cc1.get(1).getBodyUrl());
		assertEquals(adds[1], cc1.get(1).getAdditionalUrl());
		assertEquals(md5s[1], cc1.get(1).getMd5());
		assertEquals(sha256s[1], cc1.get(1).getSha256());
		assertEquals("T1-3", cc1.get(2).getTitle());
		assertEquals("A1-3", cc1.get(2).getArtist());
		assertEquals(PlayStyle.SINGLE, cc1.get(2).getPlayStyle());
		assertEquals(bodies[2], cc1.get(2).getBodyUrl());
		assertEquals(adds[2], cc1.get(2).getAdditionalUrl());
		assertEquals(md5s[2], cc1.get(2).getMd5());
		assertEquals(sha256s[2], cc1.get(2).getSha256());

		var cc2 = db2.get(ID_UPDATE2);
		assertTrue(cc2.getLastUpdateDateTime().compareTo(now) >= 0);
		assertNull(cc2.getModifiedDateTime(PlayStyle.SINGLE));
		assertNull(cc2.getModifiedDataHash(PlayStyle.SINGLE));
		assertEquals("2025-05-04T03:02:01Z", cc2.getModifiedDateTime(PlayStyle.DOUBLE).toString());
		assertEquals("3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d",
				cc2.getModifiedDataHash(PlayStyle.DOUBLE));
		assertEquals(2, cc2.getCount());
		assertEquals("T2-1", cc2.get(0).getTitle());
		assertEquals("A2-1", cc2.get(0).getArtist());
		assertEquals(PlayStyle.DOUBLE, cc2.get(0).getPlayStyle());
		assertEquals(bodies[2], cc2.get(0).getBodyUrl());
		assertEquals(adds[2], cc2.get(0).getAdditionalUrl());
		assertEquals(md5s[2], cc2.get(0).getMd5());
		assertEquals(sha256s[2], cc2.get(0).getSha256());
		assertEquals("T2-2", cc2.get(1).getTitle());
		assertEquals("A2-2", cc2.get(1).getArtist());
		assertEquals(PlayStyle.DOUBLE, cc2.get(1).getPlayStyle());
		assertEquals(bodies[1], cc2.get(1).getBodyUrl());
		assertEquals(adds[1], cc2.get(1).getAdditionalUrl());
		assertEquals(md5s[1], cc2.get(1).getMd5());
		assertEquals(sha256s[1], cc2.get(1).getSha256());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 難易度表情報の更新内容が正しい内容であること(任意項目が全てnull)
	@Test
	public void testUpdate2_ContentFile_OptionalAllNull() throws Exception {
		var db = setupUpdateDatabase((td, ps, raw) -> {
			return List.of(new ContentDescription("A", "B", ps, 0, null, null, null, null));
		});
		db.update(httpClient(), null, UpdateProgress.nop());
		var db2 = new ContentDatabase(db.getLocation(), false);
		var cc1 = db2.get(ID_UPDATE1);
		var cd = cc1.get(0);
		assertNull(cd.getBodyUrl());
		assertNull(cd.getAdditionalUrl());
		assertNull(cd.getMd5());
		assertNull(cd.getSha256());
	}

	// update(HttpClient, Duration, UpdateProgress)
	// 難易度表情報ファイルへ書き込めなかった場合、IOExceptionがスローされること
	@Test
	public void testUpdate2_ContentFile_FailedToSave() throws Exception {
		var db = setupUpdateDatabase();
		var upd1F = db.getLocation().resolve(ID_UPDATE1 + ".json");
		var ex = IOException.class;
		Files.createDirectory(upd1F);
		Files.createFile(upd1F.resolve("file"));
		assertThrows(ex, () -> db.update(httpClient(), null, UpdateProgress.nop()));
	}

	// update(HttpClient, Duration, UpdateProgress, Map<String, UpdateResult>)
	// 更新中間の難易度表で例外が発生しても後続の難易度表が更新され、全ての難易度表の更新が実施されること
	@Test
	public void testUpdate3_UpdateAll() throws Exception {
		var db = setupUpdateDatabase(r -> {
			var statusCode = r.uri().toString().equals("http://example.com/1") ? 404 : 200;
			return new UpdateResponse(statusCode, Map.of());
		});
		var results = new HashMap<String, UpdateResult>();
		results.put("dummy", new UpdateResult(UpdateResult.Type.SUCCESS));
		db.update(httpClient(), Duration.ofSeconds(1), UpdateProgress.nop(), results);
		assertEquals(2, results.size());
		var r1 = results.get(ID_UPDATE1);
		assertNotNull(r1);
		assertEquals(UpdateResult.Type.ERROR, r1.getType());
		assertNotNull(r1.getCause());
		assertFalse(r1.isSuccess());
		var r2 = results.get(ID_UPDATE2);
		assertNotNull(r2);
		assertEquals(UpdateResult.Type.SUCCESS, r2.getType());
		assertNull(r2.getCause());
		assertTrue(r2.isSuccess());
	}

	// update(HttpClient, Duration, UpdateProgress, Map<String, UpdateResult>)
	// 更新中間で割り込みを発生させるとInterruptedExceptionがスローされ、割り込み以降の更新は中止されること
	@Test
	public void testUpdate3_Interrupt() throws Exception {
		var db = setupUpdateDatabase(r -> { Thread.sleep(Long.MAX_VALUE); return null; });
		var results = new HashMap<String, UpdateResult>();
		var thrown = new Throwable[] { null };
		var thread = new Thread(() -> {
			try {
				db.update(httpClient(), Duration.ofSeconds(1), UpdateProgress.nop(),results);
			} catch (Throwable e) {
				thrown[0] = e;
			}
		});
		thread.start();
		thread.interrupt();
		thread.join();
		assertNotNull(thrown[0]);
		assertEquals(InterruptedException.class, thrown[0].getClass());
		assertEquals(2, results.size());
		var r1 = results.get(ID_UPDATE1);
		assertNotNull(r1);
		assertEquals(UpdateResult.Type.ABORT, r1.getType());
		assertNull(r1.getCause());
		assertFalse(r1.isSuccess());
		var r2 = results.get(ID_UPDATE2);
		assertNotNull(r2);
		assertEquals(UpdateResult.Type.ABORT, r2.getType());
		assertNull(r2.getCause());
		assertFalse(r2.isSuccess());
	}

	// update(HttpClient, Duration, UpdateProgress, Map<String, UpdateResult>)
	// NullPointerException client が null
	@Test
	public void testUpdate3_NullClient() throws Exception {
		var db = setupEmptyDatabase();
		var ex = NullPointerException.class;
		assertThrows(ex, () -> db.update(null, Duration.ofSeconds(1), UpdateProgress.nop(), new HashMap<>()));
	}

	// update(HttpClient, Duration, UpdateProgress, Map<String, UpdateResult>)
	// NullPointerException progress が null
	@Test
	public void testUpdate3_NullProgress() throws Exception {
		var db = setupEmptyDatabase();
		var ex = NullPointerException.class;
		assertThrows(ex, () -> db.update(httpClient(), Duration.ofSeconds(1), null, new HashMap<>()));
	}

	// update(HttpClient, Duration, UpdateProgress, Map<String, UpdateResult>)
	// NullPointerException results が null
	@Test
	public void testUpdate3_NullResults() throws Exception {
		var db = setupEmptyDatabase();
		var ex = NullPointerException.class;
		assertThrows(ex, () -> db.update(httpClient(), Duration.ofSeconds(1), UpdateProgress.nop(), null));
	}

	// update(HttpClient, Duration, UpdateProgress, Map<String, UpdateResult>)
	// UnsupportedOperationException results が変更不可のマップ
	@Test
	public void testUpdate3_UnmodifiableResults() throws Exception {
		var db = setupEmptyDatabase();
		var ex = UnsupportedOperationException.class;
		assertThrows(ex, () -> db.update(httpClient(), Duration.ofSeconds(1), UpdateProgress.nop(), Map.of()));
	}

	// update(HttpClient, Duration, UpdateProgress, Map<String, UpdateResult>)
	// IllegalStateException 読み込み排他処理エラーが発生した
	@Test
	public void testUpdate3_ReadExclusiveError() throws Exception {
		var db = setupUpdateDatabase();
		var lockF = new LockFile(db.getLocation().resolve(readLockFileName()));
		var ex = IllegalStateException.class;
		try {
			assertTrue(lockF.lock());
			assertThrows(ex, () -> db.update(httpClient(), null, UpdateProgress.nop(), new HashMap<>()));
		} finally {
			lockF.unlock();
		}
	}

	// update(HttpClient, Duration, UpdateProgress, Map<String, UpdateResult>)
	// IllegalStateException 書き込み排他処理エラーが発生した
	@Test
	public void testUpdate3_WriteExclusiveError() throws Exception {
		var db = setupUpdateDatabase();
		var lockF = new LockFile(db.getLocation().resolve(writeLockFileName()));
		var ex = IllegalStateException.class;
		try {
			assertTrue(lockF.lock());
			assertThrows(ex, () -> db.update(httpClient(), null, UpdateProgress.nop(), new HashMap<>()));
		} finally {
			lockF.unlock();
		}
	}

	private static String readLockFileName() throws Exception {
		return Tests.getsf(ContentDatabase.class, "READ_LOCK_FILE_NAME");
	}

	private static String writeLockFileName() throws Exception {
		return Tests.getsf(ContentDatabase.class, "WRITE_LOCK_FILE_NAME");
	}

	private static HttpClient httpClient() {
		return HttpClient.newBuilder().build();
	}

	private static void assertEmptyDatabase(ContentDatabase db) throws Exception {
		// データベースの内容が、作成直後の空のデータベースを表していること
		var location = db.getLocation();
		var writeLockF = location.resolve(writeLockFileName());
		assertTrue(Files.isDirectory(location));
		assertTrue(Files.exists(writeLockF));
		db.all().forEach(cc -> {
			assertNull(cc.getLastUpdateDateTime());
			assertEquals(0, cc.getCount());
		});
	}

	private static void assertCommonTestData(ContentDatabase db) throws Exception {
		// 難易度表情報ファイルがない難易度表では楽曲情報が空であること
		var id = Presets.SATELLITE.getId();
		Stream.of(Presets.values()).filter(p -> !p.getId().equals(id)).map(p -> db.get(p.getId())).forEach(cc -> {
			assertNotNull(cc);
			assertEquals(0, cc.getCount());
		});
		// 難易度表情報ファイルから難易度表情報を正しく読み込めていること
		var cc = db.get(id);
		assertNotNull(cc);
		assertEquals(id, cc.getTableDescription().getId());
		assertEquals("2025-01-02T03:04:05.600+09:00[Asia/Tokyo]", cc.getLastUpdateDateTime().toString());
		assertEquals("2025-02-03T04:05:06Z", cc.getModifiedDateTime(PlayStyle.SINGLE).toString());
		assertEquals("3333333333333333333333333333333333333333333333333333333333333333",
				cc.getModifiedDataHash(PlayStyle.SINGLE));
		assertEquals("2025-03-04T05:06:07Z", cc.getModifiedDateTime(PlayStyle.DOUBLE).toString());
		assertEquals("4444444444444444444444444444444444444444444444444444444444444444",
				cc.getModifiedDataHash(PlayStyle.DOUBLE));
		assertEquals(3, cc.getCount());
		var c1 = cc.get(0);
		assertEquals("Favorite Music", c1.getTitle());
		assertEquals("Mr.X", c1.getArtist());
		assertEquals(PlayStyle.SINGLE, c1.getPlayStyle());
		assertEquals(1, c1.getLevelIndex());
		assertEquals("http://example.com/body-fm.zip", c1.getBodyUrl().toString());
		assertEquals("http://example.com/add-fm.zip", c1.getAdditionalUrl().toString());
		assertEquals("00000000000000000000000000000000", c1.getMd5());
		assertEquals("0000000000000000000000000000000000000000000000000000000000000000", c1.getSha256());
		var c2 = cc.get(1);
		assertEquals("My Song SP", c2.getTitle());
		assertEquals("Mr.Z", c2.getArtist());
		assertEquals(PlayStyle.SINGLE, c2.getPlayStyle());
		assertEquals(3, c2.getLevelIndex());
		assertEquals("http://example.com/body-ms.zip", c2.getBodyUrl().toString());
		assertEquals("http://example.com/add-ms-sp.zip", c2.getAdditionalUrl().toString());
		assertEquals("11111111111111111111111111111111", c2.getMd5());
		assertEquals("1111111111111111111111111111111111111111111111111111111111111111", c2.getSha256());
		var c3 = cc.get(2);
		assertEquals("My Song DP", c3.getTitle());
		assertEquals("Mr.Z / obj:Mr.J", c3.getArtist());
		assertEquals(PlayStyle.DOUBLE, c3.getPlayStyle());
		assertEquals(2, c3.getLevelIndex());
		assertEquals("http://example.com/body-ms.zip", c3.getBodyUrl().toString());
		assertEquals("http://example.com/add-ms-dp.zip", c3.getAdditionalUrl().toString());
		assertEquals("22222222222222222222222222222222", c3.getMd5());
		assertEquals("2222222222222222222222222222222222222222222222222222222222222222", c3.getSha256());
	}

	private static Path setupCommonTestData() throws Exception {
		var method = Thread.currentThread().getStackTrace()[2].getMethodName();
		return setupTestData("common", method, false);
	}

	private static Path setupTestData() throws Exception {
		var method = Thread.currentThread().getStackTrace()[2].getMethodName();
		return setupTestData(method, method, false);
	}

	private static Path setupTestData(int goBack) throws Exception {
		var method = Thread.currentThread().getStackTrace()[2 + goBack].getMethodName();
		return setupTestData(method, method, false);
	}

	private static Path setupTestData(String srcName, String destName, boolean allowNoSrc) throws Exception {
		var userDir = System.getProperty("user.dir");
		var srcDir = Path.of(userDir, "src", "test", "data", "com", "lmt", "lib", "bldt", "contentdatabase", srcName);
		var destDir = sTmpDir.resolve(destName);
		if (!Files.isDirectory(srcDir)) {
			if (allowNoSrc) {
				return destDir;
			} else {
				fail(String.format("%s: No such test data directory", srcDir));
				return null;
			}
		}
		Files.walkFileTree(srcDir, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Files.createDirectories(destDir.resolve(srcDir.relativize(dir)));
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.copy(file, destDir.resolve(srcDir.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
		return destDir;
	}

	private static ContentDatabase setupEmptyDatabase() throws Exception {
		var path = sTmpDir.resolve(Thread.currentThread().getStackTrace()[2].getMethodName());
		return new ContentDatabase(path, true);
	}

	private static void setupUpdateTableDescriptions(String url, Parser parser, List<String> labels) throws Exception {
		var actualUrl = nonNull(url) ? url : "http://example.com";
		var actualParser = nonNull(parser) ? parser : TableDescriptionTest.EMPTY_PARSER;
		var actualLabels = nonNull(labels) ? labels : List.of("0", "1", "2");
		var sp = new PlayStyleDescription("s", new URL(actualUrl + "/1"), actualLabels);
		var dp = new PlayStyleDescription("d", new URL(actualUrl + "/2"), actualLabels);
		var td1 = new TableDescription(ID_UPDATE1, "Update1", new URL(actualUrl + "/home1"), actualParser, sp, null);
		var td2 = new TableDescription(ID_UPDATE2, "Update2", new URL(actualUrl + "/home2"), actualParser, null, dp);
		Map<String, TableDescription> descs = Tests.getsf(DifficultyTables.class, "sTableDescs");
		descs.clear();
		descs.put(td1.getId(), td1);
		descs.put(td2.getId(), td2);
	}

	private static ContentDatabase setupUpdateDatabaseCore(UpdateSender sender, Parser parser) throws Exception {
		setupUpdateTableDescriptions(null, parser, null);
		var method = Thread.currentThread().getStackTrace()[3].getMethodName();
		var path = setupTestData(method, method, true);
		return new UpdateDatabase(path, sender);
	}

	private static ContentDatabase setupUpdateDatabase(UpdateSender sender, Parser parser) throws Exception {
		return setupUpdateDatabaseCore(sender, parser);
	}

	private static ContentDatabase setupUpdateDatabase(UpdateSender sender) throws Exception {
		return setupUpdateDatabaseCore(sender, (td, ps, raw) -> List.of());
	}

	private static ContentDatabase setupUpdateDatabase(Parser parser)
			throws Exception {
		return setupUpdateDatabaseCore(r -> new UpdateResponse(200, Map.of()), parser);
	}

	private static ContentDatabase setupUpdateDatabase() throws Exception {
		return setupUpdateDatabaseCore(r -> new UpdateResponse(200, Map.of()), (td, ps, raw) -> List.of());
	}
}
