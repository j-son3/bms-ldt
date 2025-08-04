package com.lmt.lib.bldt;

import static com.lmt.lib.bldt.DifficultyTables.*;
import static com.lmt.lib.bldt.internal.Assertion.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.lmt.lib.bldt.internal.LockFile;
import com.lmt.lib.bldt.internal.Utility;

/**
 * 難易度表データベースを表すクラスです。
 *
 * <p>難易度表データベースは複数の難易度表情報ファイルの集合体であり、1個のディレクトリ内で集約・管理されます。
 * 通常、難易度表データベースはLDTライブラリ既定のディレクトリ({@link DifficultyTables#DEFAULT_LOCATION})
 * に記録されますが、アプリケーション指定の固有のディレクトリを指定することも可能です。</p>
 *
 * <p>難易度表情報は更新処理によって各難易度表を保管するサーバーからインターネット経由でダウンロードされ、
 * 当ライブラリ提唱の統一データ形式でローカルマシン内に保存されます。通常、保存された難易度表情報は当ライブラリを
 * 経由して読み込まれ、当クラスを含む各APIによって情報にアクセスすることができます。
 * 当ライブラリを経由せずに難易度表情報にアクセスする場合は統一データ形式に準拠する形で情報を読み込み、
 * 各情報項目を適切な用途で使用する必要があります。</p>
 *
 * @since 0.1.0
 */
public class ContentDatabase {
	/** デフォルトの難易度表データベース格納先パス */
	static Path DEFAULT_LOCATION = DifficultyTables.DEFAULT_LOCATION;
	/** 難易度表データベースのバージョン */
	public static final int VERSION = 1;
	/** 読み込み排他用ロックファイル名 */
	private static final String READ_LOCK_FILE_NAME = ".read.lock";
	/** 書き込み排他用ロックファイル名 */
	private static final String WRITE_LOCK_FILE_NAME = ".write.lock";

	/** 難易度表データベース格納先パス */
	private Path mLocation;
	/** 難易度表情報マップ */
	private Map<String, ContentCollection> mCollections;
	/** データ読み取り用ロックファイル */
	private LockFile mReadLock;
	/** データ書き込み用ロックファイル */
	private LockFile mWriteLock;

	/**
	 * 新しい難易度表データベースオブジェクトを構築します。
	 * <p>当コンストラクタを使用すると、格納先パスを {@link DifficultyTables#DEFAULT_LOCATION} と解釈し、
	 * このパスにディレクトリが存在しない場合は新しく作成します。</p>
	 * <p>コンストラクタの詳しい動作については {@link #ContentDatabase(Path, boolean)} を参照してください。</p>
	 * @throws IOException ファイル・ディレクトリ入出力エラーが発生した
	 * @since 0.1.0
	 */
	public ContentDatabase() throws IOException {
		processLoad(DEFAULT_LOCATION, true);
	}

	/**
	 * 新しい難易度表データベースオブジェクトを構築します。
	 * <p>オブジェクトが構築される時、指定された格納先パスから各難易度表情報を読み込み、メモリ上にロードします。
	 * 難易度表情報ファイルが存在しない場合は空の難易度表情報としてロードされます。</p>
	 * <p>格納先パスに指定のディレクトリは必ず存在していなければなりません。存在しない場合は例外をスローします。
	 * 2番目の引数に true を指定するとディレクトリが存在しない場合にディレクトリが新しく作成されます。
	 * この場合、全ての難易度表情報が空となります。</p>
	 * <p>{@link DifficultyTables#add(TableDescription)} で難易度表定義を追加している場合、
	 * その難易度表情報も読み込もうとします。</p>
	 * <p>難易度表情報の読み込み中にファイル破損を検出したり入出力エラーが発生した場合は例外をスローします。
	 * その場合、読み込み途中のデータは全て破棄されます。</p>
	 * <p>難易度表情報の読み込み中は難易度表データベースの書き込みはロックされ、同データベースに対しての更新処理は
	 * 全て失敗します。その点の詳細については {@link #update(HttpClient, Duration, UpdateProgress)} を参照してください。</p>
	 * @param location 難易度表データベースの格納先パス
	 * @param createIfNeeded location に指定のディレクトリが存在しない場合に新しく作成するかどうか
	 * @throws NullPointerException location が null
	 * @throws NoSuchFileException 難易度表データベース格納先パスが存在しない
	 * @throws NoSuchFileException 難易度表データベース格納先パスがファイル
	 * @throws IOException 入出力エラーが発生した
	 * @throws IOException 難易度表データベース読み込み中にファイル破損、データ改ざんを検出した
	 * @throws IllegalStateException 難易度表データベースの書き込みがロックされている(データベース更新中)
	 * @since 0.1.0
	 */
	public ContentDatabase(Path location, boolean createIfNeeded) throws IOException {
		assertArgNotNull(location, "location");
		processLoad(location, createIfNeeded);
	}

	/**
	 * 難易度表データベース格納先パスを取得します。
	 * @return 難易度表データベース格納先パス
	 * @since 0.1.0
	 */
	public Path getLocation() {
		return mLocation;
	}

	/**
	 * 全ての難易度表情報を走査するストリームを返します。
	 * @return 全ての難易度表情報を走査するストリーム
	 * @since 0.1.0
	 */
	public Stream<ContentCollection> all() {
		return mCollections.values().stream();
	}

	/**
	 * 指定したIDに該当する難易度表情報を取得します。
	 * @param id ID
	 * @return 難易度表情報、該当する難易度表情報が存在しない場合は null
	 * @throws NullPointerException id が null
	 * @since 0.1.0
	 */
	public ContentCollection get(String id) {
		assertArgNotNull(id, "id");
		return mCollections.get(id);
	}

	/**
	 * 難易度表データベースの更新を行います。
	 * <p>当メソッドは指定されたIDの難易度表定義のみを更新する点を除き、
	 * {@link #update(HttpClient, Duration, UpdateProgress)} と同様の動作を行います。</p>
	 * @param client HTTP通信に使用するクライアントオブジェクト
	 * @param id 更新対象の難易度表定義のID
	 * @param timeout 楽曲情報データダウンロード時のサーバー応答タイムアウト。null の場合タイムアウトなし。
	 * @param progress 更新処理の進捗情報を報告するハンドラオブジェクト
	 * @throws NullPointerException client が null
	 * @throws NullPointerException id が null
	 * @throws NullPointerException progress が null
	 * @throws IllegalArgumentException id に該当する難易度表定義が存在しない
	 * @throws HttpTimeoutException HTTP通信で接続・応答タイムアウトが発生した
	 * @throws IOException HTTP通信で送受信エラーが発生した
	 * @throws InterruptedException スレッド割り込みによる更新処理の中止が発生した
	 * @throws IllegalStateException 読み書き排他処理エラーが発生した
	 * @since 0.1.0
	 */
	public void update(HttpClient client, String id, Duration timeout, UpdateProgress progress)
			throws IOException, InterruptedException {
		assertArgNotNull(client, "client");
		assertArgNotNull(id, "id");
		assertArgNotNull(progress, "progress");

		var tableDesc = DifficultyTables.get(id);
		assertArg(Objects.nonNull(tableDesc), "No difficulty table with such ID: %s", id);

		try {
			lock(true, true);
			processUpdate(client, tableDesc, 0, 1, timeout, progress);
		} finally {
			unlock(true, true);
		}
	}

	/**
	 * 難易度表データベースの更新を行います。
	 * <p>当メソッドを実行すると、{@link DifficultyTables#all()} で取得できる全ての難易度表定義を使用し、
	 * 楽曲情報のダウンロード～難易度表情報ファイルの更新を行います。</p>
	 * <p>更新処理が開始されると難易度表データベースは読み書きの両方がロックされ、同データベースの読み込みと更新の
	 * 両方が排他状態となります。その間、同データベースでの難易度表データベースオブジェクト構築、
	 * および当メソッドの他プロセス・スレッドからの呼び出しは全て失敗します。</p>
	 * <p>当メソッドではインターネット経由でHTTP通信を行い、楽曲情報のダウンロードを行います。
	 * 通信設定は引数のHTTPクライアントオブジェクトを通して予め実施しておいてください。
	 * (例えば、プロキシの仕様有無やリダイレクトフォローなど)</p>
	 * <p>楽曲情報のダウンロードでは、最初にダウンロード対象データの更新日時を検証します。
	 * データが更新されておらず更新日時に変化がない場合は前回の更新からデータ差分がないと見なしダウンロードを中止します。
	 * データをダウンロード後、前回更新時のデータとの比較を行い、同じデータと判定された場合は更新処理を中止します。</p>
	 * <p>ダウンロードされたデータは難易度表定義に登録されたパーサ({@link Parser})を使用して楽曲情報が解析され、
	 * その結果が難易度表情報ファイルに記録され、メモリ上の楽曲情報も同様の内容に更新されます。</p>
	 * <p>当メソッドは登録済みの全ての難易度表を更新しようとするため、処理完了までに非常に時間がかかります。
	 * 更新処理は別スレッドを使用した並列処理とすることを推奨します。当メソッド実行中はスレッドの割り込みを監視し、
	 * 割り込みを検出した時に処理を中止し InterruptedException をスローします。その場合、処理中だった難易度表は
	 * 難易度表情報ファイルには保存されず、割り込み検出以前の更新結果が更新前に戻ることはありません。</p>
	 * <p>難易度表ごとに個別に更新を行いたい場合は {@link #update(HttpClient, String, Duration, UpdateProgress)}
	 * を使用してください。</p>
	 * @param client HTTP通信に使用するクライアントオブジェクト
	 * @param timeout 楽曲情報データダウンロード時のサーバー応答タイムアウト。null の場合タイムアウトなし。
	 * @param progress 更新処理の進捗情報を報告するハンドラオブジェクト
	 * @throws NullPointerException client が null
	 * @throws NullPointerException progress が null
	 * @throws HttpTimeoutException HTTP通信で接続・応答タイムアウトが発生した
	 * @throws IOException HTTP通信で送受信エラーが発生した
	 * @throws InterruptedException スレッド割り込みによる更新処理の中止が発生した
	 * @throws IllegalStateException 読み書き排他処理エラーが発生した
	 * @since 0.1.0
	 */
	public void update(HttpClient client, Duration timeout, UpdateProgress progress)
			throws IOException, InterruptedException {
		assertArgNotNull(client, "client");
		assertArgNotNull(progress, "progress");
		try {
			lock(true, true);
			var tableDescs = DifficultyTables.all().collect(Collectors.toList());
			var numDesc = tableDescs.size();
			for (var i = 0; i < numDesc; i++) {
				processUpdate(client, tableDescs.get(i), i, numDesc, timeout, progress);
			}
		} finally {
			unlock(true, true);
		}
	}

	/**
	 * 難易度表データベースの更新を行います。
	 * <p>当メソッドを実行すると、{@link DifficultyTables#all()} で取得できる全ての難易度表定義を使用し、
	 * 楽曲情報のダウンロード～難易度表情報ファイルの更新を行います。</p>
	 * <p>更新処理が開始されると難易度表データベースは読み書きの両方がロックされ、同データベースの読み込みと更新の
	 * 両方が排他状態となります。その間、同データベースでの難易度表データベースオブジェクト構築、
	 * および当メソッドの他プロセス・スレッドからの呼び出しは全て失敗します。</p>
	 * <p>当メソッドではインターネット経由でHTTP通信を行い、楽曲情報のダウンロードを行います。
	 * 通信設定は引数のHTTPクライアントオブジェクトを通して予め実施しておいてください。
	 * (例えば、プロキシの仕様有無やリダイレクトフォローなど)</p>
	 * <p>楽曲情報のダウンロードでは、最初にダウンロード対象データの更新日時を検証します。
	 * データが更新されておらず更新日時に変化がない場合は前回の更新からデータ差分がないと見なしダウンロードを中止します。
	 * データをダウンロード後、前回更新時のデータとの比較を行い、同じデータと判定された場合は更新処理を中止します。</p>
	 * <p>ダウンロードされたデータは難易度表定義に登録されたパーサ({@link Parser})を使用して楽曲情報が解析され、
	 * その結果が難易度表情報ファイルに記録され、メモリ上の楽曲情報も同様の内容に更新されます。</p>
	 * <p>当メソッドは登録済みの全ての難易度表を更新しようとするため、処理完了までに非常に時間がかかります。
	 * 更新処理は別スレッドを使用した並列処理とすることを推奨します。当メソッド実行中はスレッドの割り込みを監視し、
	 * 割り込みを検出した時に処理を中止し InterruptedException をスローします。その場合、処理中だった難易度表は
	 * 難易度表情報ファイルには保存されず、割り込み検出以前の更新結果が更新前に戻ることはありません。</p>
	 * <p>当メソッドではスレッドの割り込み検出と入力パラメータ不備、および状態エラーの場合を除き、
	 * 更新途中で実行時例外がスローされても更新は停止されません。難易度表ごとの更新結果は入力パラメータ
	 * results のマップに格納されますので、必要に応じて参照するようにしてください。このマップのキーは難易度表IDです。</p>
	 * <p>難易度表ごとに個別に更新を行いたい場合は {@link #update(HttpClient, String, Duration, UpdateProgress)}
	 * を使用してください。</p>
	 * @param client HTTP通信に使用するクライアントオブジェクト
	 * @param timeout 楽曲情報データダウンロード時のサーバー応答タイムアウト。null の場合タイムアウトなし。
	 * @param progress 更新処理の進捗情報を報告するハンドラオブジェクト
	 * @param results 更新処理の結果を格納するマップ
	 * @throws NullPointerException client が null
	 * @throws NullPointerException progress が null
	 * @throws NullPointerException results が null
	 * @throws UnsupportedOperationException results が変更不可のマップ
	 * @throws InterruptedException スレッド割り込みによる更新処理の中止が発生した
	 * @throws IllegalStateException 読み書き排他処理エラーが発生した
	 * @since 0.2.0
	 */
	public void update(HttpClient client, Duration timeout, UpdateProgress progress, Map<String, UpdateResult> results)
			throws InterruptedException {
		assertArgNotNull(client, "client");
		assertArgNotNull(progress, "progress");
		assertArgNotNull(results, "results");
		results.clear();
		try {
			lock(true, true);
			var tableDescs = DifficultyTables.all().collect(Collectors.toList());
			var numDesc = tableDescs.size();
			for (var i = 0; i < numDesc; i++) {
				var td = tableDescs.get(i);
				try {
					// 指定した難易度表の更新を実行する
					processUpdate(client, td, i, numDesc, timeout, progress);
					results.put(td.getId(), new UpdateResult(UpdateResult.Type.SUCCESS));
				} catch (InterruptedException e) {
					// スレッド割り込みを検知した場合は未更新分の難易度表の結果を全て「中止」とする
					for (var j = i; j < numDesc; j++) {
						results.put(tableDescs.get(j).getId(), new UpdateResult(UpdateResult.Type.ABORT));
					}
					throw e;
				} catch (Exception e) {
					// エラーが発生した場合はその難易度表の結果を「エラー」とする
					results.put(td.getId(), new UpdateResult(e));
				}
			}
		} finally {
			unlock(true, true);
		}
	}

	/**
	 * HTTPのリクエスト送受信
	 * @param client HTTPクライアントオブジェクト
	 * @param request リクエスト内容
	 * @return レスポンス内容
	 * @throws HttpTimeoutException HTTP通信で接続・応答タイムアウトが発生した
	 * @throws IOException HTTP通信で送受信エラーが発生した
	 * @throws InterruptedException スレッド割り込みが発生した
	 */
	HttpResponse<InputStream> send(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
		return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
	}

	/**
	 * 難易度表データベース読み込み処理
	 * @param location 難易度表データベースの格納先パス
	 * @param createIfNeeded location に指定のディレクトリが存在しない場合に新しく作成するかどうか
	 * @throws IOException 入出力エラーが発生した
	 */
	private void processLoad(Path location, boolean createIfNeeded) throws IOException {
		printLog("LOAD: Location='%s', create=%s", location, createIfNeeded);

		// DB格納先フォルダのチェック
		mLocation = location;
		if (!Files.isDirectory(mLocation)) {
			if (createIfNeeded) {
				// DB格納先フォルダを作成する
				printLog("Location is not found and try create");
				Files.createDirectories(mLocation);
			} else {
				// DB格納先フォルダなしで作成しない場合はエラー扱いとする
				throw new NoSuchFileException(mLocation.toString(), null, "Nothing location but not be created");
			}
		}

		// DB制御で使用するロックファイルを作成する
		// これから読み込み処理を行うので、その間は書き込みをロックする
		mReadLock = new LockFile(mLocation.resolve(READ_LOCK_FILE_NAME));
		mWriteLock = new LockFile(mLocation.resolve(WRITE_LOCK_FILE_NAME));
		try {
			// 書き込みをロックする
			lock(false, true);

			// 難易度表定義に基づいて楽曲情報をファイルから読み込む
			mCollections = new LinkedHashMap<String, ContentCollection>();
			var iSp = PlayStyle.SINGLE.ordinal();
			var iDp = PlayStyle.DOUBLE.ordinal();
			var modifiedDateTimes = new ZonedDateTime[PlayStyle.COUNT];
			var modifiedDataHashes = new String[PlayStyle.COUNT];
			var tableDescs = DifficultyTables.all().collect(Collectors.toList());
			for (var tableDesc : tableDescs) {
				// 楽曲情報を構成する情報の初期値を生成する
				var id = tableDesc.getId();
				var contents = new ArrayList<ContentDescription>();
				printLog("Loading '%s'...", id);

				// 楽曲情報のJSONファイルから情報を読み込む
				var contentFileName = String.format("%s.json", tableDesc.getId());
				var contentFilePath = mLocation.resolve(contentFileName);
				if (!Files.isRegularFile(contentFilePath)) {
					// 該当するファイルが存在しない、またはファイルとして読み込めない場合はスキップ
					printLog("Skip load because database file is not found: Path='%s'", contentFilePath);
					var emptyCollection = new ContentCollection(
							tableDesc, null, null, null, null, null, contents);
					mCollections.put(id, emptyCollection);
					continue;
				}

				try {
					// 楽曲情報のJSONを解析後、ソースコードを直ちに破棄する
					printLog("Read and parse database: Path='%s'", contentFilePath);
					var jsonSource = new String(Files.readAllBytes(contentFilePath), StandardCharsets.UTF_8);
					var root = new JSONObject(jsonSource);
					jsonSource = null;

					// 難易度表データベースのバージョンを確認する
					// ※現状、バージョンは1以外有り得ないので、それ以外の値は改ざんと判定する
					// ※将来的に、必要に応じてマイグレーション処理が行われる想定
					var inVersion = root.getInt("version");
					if (inVersion != VERSION) {
						printLog("Invalid version: Value='%s'", inVersion);
						tampering(contentFilePath, null);
					}

					// IDが難易度表情報と一致していること
					var inId = root.getString("id");
					if (!id.equals(inId)) {
						printLog("Invalid ID: Value='%s'", inId);
						tampering(contentFilePath, null);
					}

					// データ更新日時を取得する
					var inLastUpdated = root.getString("lastUpdated");
					var lastUpdateDateTime = (ZonedDateTime)null;
					try {
						lastUpdateDateTime = ZonedDateTime.parse(inLastUpdated);
					} catch (DateTimeParseException e) {
						printLog("Invalid lastUpdated: Value='%s'", inLastUpdated);
						tampering(contentFilePath, e);
					}

					// 最終更新情報を解析する
					Arrays.fill(modifiedDateTimes, null);
					Arrays.fill(modifiedDataHashes, null);
					var inModifiedList = root.getJSONArray("modified");
					if (inModifiedList.length() != PlayStyle.COUNT) {
						// 最終更新情報のデータ構成がおかしい(個数が合わない)場合は改ざんと見なす
						printLog("Invalid modified: Length=%d", inModifiedList.length());
						tampering(contentFilePath, null);
					}
					for (var i = 0; i < PlayStyle.COUNT; i++) {
						// 最終更新日時を取得する(nullの場合もある)
						var inModified = inModifiedList.getJSONObject(i);
						var inDateTime = inModified.get("dateTime");
						if (Utility.isJsonNull(inDateTime)) {
							modifiedDateTimes[i] = null;
						} else {
							try {
								modifiedDateTimes[i] = ZonedDateTime.parse(inDateTime.toString());
							} catch (DateTimeParseException e) {
								printLog("Invalid modified[%d].dateTime: Value='%s'", i, inDateTime);
								tampering(contentFilePath, e);
							}
						}

						// 最終更新データハッシュを取得する(nullの場合もある)
						var inDataHash = inModified.get("dataHash");
						if (Utility.isJsonNull(inDataHash)) {
							modifiedDataHashes[i] = null;
						} else if (Utility.isSha256(inDataHash.toString())) {
							modifiedDataHashes[i] = inDataHash.toString();
						} else {
							printLog("Invalid modified[%d].dataHash: Value='%s'", i, inDataHash);
							tampering(contentFilePath, null);
						}
					}

					// 楽曲情報リストを取得する
					var inContents = root.getJSONArray("contents");
					var numContents = inContents.length();
					for (var i = 0; i < numContents; i++) {
						// 楽曲情報1件を取得する(objectでない場合は当該データを無視する)
						var inContent = inContents.optJSONObject(i);
						if (Objects.isNull(inContent)) {
							printLog("contents[%d]: Skip because it's not object type", i);
							continue;
						}

						// タイトルとアーティストを取得する
						var inTitle = inContent.optString("title", "");
						var inArtist = inContent.optString("artist", "");
						if (inTitle.isEmpty()) {
							// タイトルが未定義、空文字の楽曲情報は不正データとする
							printLog("contents[%d]: Skip because invalid title", i);
							continue;
						}
						if (inArtist.isEmpty() && !inContent.has("artist")) {
							// アーティストが未定義の場合は不正データとする ※空文字は許容
							printLog("contents[%d]: Skip because artist not found", i);
							continue;
						}

						// DPモードかどうかを取得する
						var inPlayStyle = PlayStyle.fromBoolean(inContent.optBoolean("dpMode", false));
						var styleDesc = tableDesc.getPlayStyleDescription(inPlayStyle);
						if (Objects.isNull(styleDesc)) {
							// 難易度表が該当プレースタイルに非対応の場合は不正データとする
							printLog("contents[%d]: Skip because un-supported play style", i);
							continue;
						}

						// 難易度表インデックスを取得する
						var inLevelIndex = inContent.optInt("levelIndex", -1);
						if ((inLevelIndex < 0) || (inLevelIndex >= styleDesc.getLabels().size())) {
							// 難易度表インデックスが有効範囲外の場合は不正データとする
							printLog("contents[%d]: Skip because levelIndex out of range: Value=%d", i, inLevelIndex);
							continue;
						}

						// その他の任意情報を解析する
						var fi = i;
						var inBodyUrl = Utility.optionalJsonUrl(inContent.optString("bodyUrl", ""), v -> {
							printLog("contents[%d]: Invalid body URL: Value='%s'", fi, v);
						});
						var inAddUrl = Utility.optionalJsonUrl(inContent.optString("additionalUrl", ""), v -> {
							printLog("contents[%d]: Invalid additional URL: Value='%s'", fi, v);
						});
						var inMd5 = Utility.optionalJsonHash(inContent.optString("md5"), Utility::isMd5, v -> {
							printLog("contents[%d]: Invalid MD5: Value='%s'", fi, v);
						});
						var inSha256 = Utility.optionalJsonHash(inContent.optString("sha256"), Utility::isSha256, v -> {
							printLog("contents[%d]: Invalid SHA-256: Value='%s'", fi, v);
						});

						// 楽曲情報を登録する
						contents.add(new ContentDescription(
								inTitle, inArtist, inPlayStyle, inLevelIndex,
								inBodyUrl, inAddUrl, inMd5, inSha256));
					}

					// 難易度表情報を登録する
					var collection = new ContentCollection(
							tableDesc,
							lastUpdateDateTime,
							modifiedDateTimes[iSp], modifiedDataHashes[iSp],
							modifiedDateTimes[iDp], modifiedDataHashes[iDp],
							contents);
					mCollections.put(id, collection);
					printLog("Load '%s' complete", id);
				} catch (IOException e) {
					// IOExceptionはそのままスロー
					throw e;
				} catch (Exception e) {
					// JSON解析中のエラーはデータ破損としてIOExceptionをスローする
					printLog("Un-expected exception: %s", e.getMessage());
					var msg = String.format("%s: Broken database", contentFilePath);
					throw new IOException(msg, e);
				}
			}
			printLog("LOAD '%s' complete", location);
		} finally {
			// 書き込みロックを解除する
			unlock(false, true);
		}
	}

	/**
	 * 難易度表データベース更新処理
	 * @param client HTTPクライアントオブジェクト
	 * @param tableDesc 難易度表定義
	 * @param iDesc 更新対象の難易度表定義のインデックス値
	 * @param numDesc 更新対象の難易度表定義の数
	 * @param timeout 楽曲情報データダウンロード時のサーバー応答タイムアウト。null の場合タイムアウトなし。
	 * @param progress 更新処理の進捗情報を報告するハンドラオブジェクト
	 * @throws HttpTimeoutException HTTP通信で接続・応答タイムアウトが発生した
	 * @throws IOException HTTP通信で送受信エラーが発生した
	 * @throws InterruptedException スレッド割り込みによる更新処理の中止が発生した
	 * @throws IllegalStateException 読み書き排他処理エラーが発生した
	 */
	private void processUpdate(HttpClient client, TableDescription tableDesc, int iDesc, int numDesc,
			Duration timeout, UpdateProgress progress) throws IOException, InterruptedException {
		printLog("UPDATE: ID='%s', Name='%s', Desc=%d/%d, Timeout=%s",
				tableDesc.getId(), tableDesc.getName(), iDesc, numDesc, timeout);

		// SP/DPの楽曲情報をWebサーバからダウンロードする
		var dirty = false;
		var outModifiedDateTimes = new ZonedDateTime[PlayStyle.COUNT];
		var outModifiedDataHashes = new String[PlayStyle.COUNT];
		var outContents = new ArrayList<List<ContentDescription>>();
		IntStream.range(0, PlayStyle.COUNT).forEach(i -> outContents.add(new ArrayList<>()));
		for (var playStyle : PlayStyle.values()) {
			// 当該難易度表で非対応のプレースタイルはスキップする
			var styleDesc = tableDesc.getPlayStyleDescription(playStyle);
			if (Objects.isNull(styleDesc)) {
				printLog("Play style='%s': Unsupported", playStyle);
				continue;
			} else {
				printLog("Play style='%s': Supported", playStyle);
				progress.publish(tableDesc, playStyle, iDesc, numDesc, UpdateProgress.Status.START);
			}

			// 最終更新情報を取得する
			var modDateTime = (ZonedDateTime)null;
			var modDataHash = (String)null;
			var collection = mCollections.get(tableDesc.getId());
			if (Objects.nonNull(collection)) {
				modDateTime = collection.getModifiedDateTime(playStyle);
				modDataHash = collection.getModifiedDataHash(playStyle);
				printLog("Current modified: DateTime='%s', Hash=%s", modDateTime, modDataHash);
			} else {
				printLog("Current modified: None because collection is not found");
			}

			// 更新前のデータを予め設定しておく
			// 更新不要の場合、以下の値がそのままデータベースに入ることとなる
			outModifiedDateTimes[playStyle.ordinal()] = modDateTime;
			outModifiedDataHashes[playStyle.ordinal()] = modDataHash;
			outContents.set(playStyle.ordinal(), collection.all()
					.filter(c -> c.getPlayStyle() == playStyle)
					.collect(Collectors.toList()));

			// 楽曲情報URLを生成する
			var contentUri = (URI)null;
			try {
				printLog("Content URL='%s'", styleDesc.getContentUrl());
				contentUri = styleDesc.getContentUrl().toURI();
			} catch (URISyntaxException e) {
				printLog("Bad content URL: %s", e.getMessage());
				progress.publish(tableDesc, playStyle, iDesc, numDesc, UpdateProgress.Status.ERROR);
				var msg = String.format("%s: Can't use this content URL", styleDesc.getContentUrl());
				throw new IOException(msg, e);
			}

			// リクエストを生成する
			var reqBuilder = HttpRequest.newBuilder().GET().uri(contentUri);
			if (Objects.nonNull(timeout)) {
				// リクエストタイムアウトが設定されている場合はタイムアウト時間を設定する
				reqBuilder.timeout(timeout);
			}
			if (Objects.nonNull(modDateTime)) {
				// 最終更新日時が判明している場合は If-Modified-Since を設定する
				reqBuilder.header("If-Modified-Since", modDateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME));
			}

			// 楽曲情報の元データ取得リクエストを送信する
			printLog("Waiting response ...");
			var resp = send(client, reqBuilder.build());
			var statusCode = resp.statusCode();
			printLog("Response=%d", statusCode);
			if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
				// 最終更新日時から内容が変更されていない場合は何もしない
				progress.publish(tableDesc, playStyle, iDesc, numDesc, UpdateProgress.Status.UNNECESSARY);
				continue;
			} else if (statusCode != HttpURLConnection.HTTP_OK) {
				// その他、正常受信以外の場合はエラーとする
				progress.publish(tableDesc, playStyle, iDesc, numDesc, UpdateProgress.Status.ERROR);
				var msg = String.format("Received %d from '%s'", statusCode, contentUri);
				throw new IOException(msg);
			} else {
				// Do nothing
			}

			// 楽曲情報元データ受信処理
			// この処理は通信環境の影響で時間がかかる場合があるためスレッド割り込みを監視する
			printLog("Receiving body ...");
			var stream = resp.body();
			var rcv = 0;
			var rcvBuffer = new byte[4096];
			var rawBuffer = new ByteArrayOutputStream(65536);
			while ((rcv = stream.read(rcvBuffer)) != -1) {
				rawBuffer.write(rcvBuffer, 0, rcv);
				if (Thread.currentThread().isInterrupted()) {
					// データ受信中にスレッドが割り込まれた場合は処理を中止する
					throw new InterruptedException();
				}
			}
			var raw = rawBuffer.toByteArray();
			rcvBuffer = null;
			rawBuffer = null;
			printLog("Received: Length=%dbytes", raw.length);

			// 楽曲情報元データの最終更新日時を取得する
			var respModDateTime = resp.headers().firstValue("Last-Modified");
			if (respModDateTime.isPresent()) {
				printLog("Last-modified='%s'", respModDateTime.get());
				try {
					// 最終更新日時が応答されている場合はそれを記録する
					var dt = ZonedDateTime.parse(respModDateTime.get(), DateTimeFormatter.RFC_1123_DATE_TIME);
					outModifiedDateTimes[playStyle.ordinal()] = dt;
					dirty = true;
				} catch (DateTimeParseException e) {
					// Webサーバが下手こいて変な日時を返した場合の処遇は知らん
					printLog("Bad Last-modified: %s", e.getMessage());
				}
			} else {
				// 楽曲情報元データの最終更新日時が含まれない場合は元の日時を更新しない
				printLog("Last-modified was not presented");
			}

			// 楽曲情報元データのハッシュ値を計算する
			try {
				var hash = MessageDigest.getInstance("SHA-256").digest(raw);
				var sha256 = Utility.byteArrayToString(hash);
				printLog("Content-hash: %s", sha256);
				if (Objects.nonNull(modDataHash) && modDataHash.equalsIgnoreCase(sha256)) {
					// 受信データのハッシュ値が最終更新データハッシュと一致する場合は更新しない
					progress.publish(tableDesc, playStyle, iDesc, numDesc, UpdateProgress.Status.UNNECESSARY);
					continue;
				} else {
					// 次回更新時のハッシュ値チェックのために、計算したハッシュ値を記録する
					printLog("Changed content or first update");
					outModifiedDataHashes[playStyle.ordinal()] = sha256;
					dirty = true;
				}
			} catch (NoSuchAlgorithmException e) {
				// Don't care
			}

			// 受信データから楽曲情報を解析する
			printLog("Parsing content...");
			var parser = tableDesc.getParser();
			var contents = (List<ContentDescription>)null;
			try {
				contents = parser.parse(tableDesc, playStyle, raw);
			} catch (IOException e) {
				progress.publish(tableDesc, playStyle, iDesc, numDesc, UpdateProgress.Status.ERROR);
				throw e;
			} catch (Exception e) {
				progress.publish(tableDesc, playStyle, iDesc, numDesc, UpdateProgress.Status.ERROR);
				throw new IOException("Parser thrown an exception", e);
			}

			// 解析結果を確認する
			if (Thread.currentThread().isInterrupted()) {
				// 解析処理中にスレッドが割り込まれた場合は処理を中止する
				printLog("Interrupted");
				throw new InterruptedException();
			} else if (Objects.isNull(contents)) {
				// パーサがnullを返した場合は異常終了とする
				printLog("Parser returned null");
				progress.publish(tableDesc, playStyle, iDesc, numDesc, UpdateProgress.Status.ERROR);
				var msg = String.format("Parser '%s' returned null contents", parser.getClass().getName());
				throw new IOException(msg);
			} else {
				// 解析した楽曲情報をリストに追記する
				// リストは最終的にSP/DP混合のリストになる
				printLog("Parse complete: Count=%d", contents.size());
				outContents.set(playStyle.ordinal(), contents);
				dirty = true;
				progress.publish(tableDesc, playStyle, iDesc, numDesc, UpdateProgress.Status.DONE);
			}
			printLog("Play style='%s' Done", playStyle);
		}

		// 難易度表データベースの更新がない場合は何もしない
		if (!dirty) {
			printLog("UPDATE '%s' un-necessary update", tableDesc.getId());
			return;
		}

		// 難易度表情報のJSONを構築する
		printLog("Writing database...");
		var jsonObj = new JSONObject();
		var lastUpdateDateTime = ZonedDateTime.now();
		jsonObj.put("version", VERSION);
		jsonObj.put("id", tableDesc.getId());
		jsonObj.put("lastUpdated", lastUpdateDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));

		// 最終更新情報を生成する
		var jsonModArray = new JSONArray();
		for (var i = 0; i < PlayStyle.COUNT; i++) {
			var jsonMod = new JSONObject();
			var dt = outModifiedDateTimes[i];
			jsonMod.put("dateTime", Utility.valueOrJsonNull(dt, d -> d.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)));
			jsonMod.put("dataHash", Utility.valueOrJsonNull(outModifiedDataHashes[i], d -> d));
			jsonModArray.put(jsonMod);
		}
		jsonObj.put("modified", jsonModArray);

		// 楽曲情報をマージする
		var allContents = new ArrayList<ContentDescription>();
		outContents.stream().forEach(allContents::addAll);
		outContents.clear();

		// 楽曲情報リストを生成する
		var jsonContentsArray = new JSONArray();
		for (var content : allContents) {
			var jsonContent = new JSONObject();
			jsonContent.put("title", content.getTitle());
			jsonContent.put("artist", content.getArtist());
			jsonContent.put("dpMode", content.getPlayStyle() == PlayStyle.DOUBLE);
			jsonContent.put("levelIndex", content.getLevelIndex());
			jsonContent.put("bodyUrl", Utility.valueOrJsonNull(content.getBodyUrl(), URL::toString));
			jsonContent.put("additionalUrl", Utility.valueOrJsonNull(content.getAdditionalUrl(), URL::toString));
			jsonContent.put("md5", Utility.valueOrJsonNull(content.getMd5(), i -> i));
			jsonContent.put("sha256", Utility.valueOrJsonNull(content.getSha256(), i -> i));
			jsonContentsArray.put(jsonContent);
		}
		jsonObj.put("contents", jsonContentsArray);

		// 難易度表情報のJSONを一時ファイルに保存する
		var tmpPath = mLocation.resolve(".tmp");
		var jsonObjStr = jsonObj.toString(2);
		jsonObj = null;
		Files.writeString(tmpPath, jsonObjStr, StandardCharsets.UTF_8);

		// 新しい難易度表情報に置き換える
		var filePath = mLocation.resolve(String.format("%s.json", tableDesc.getId()));
		Files.move(tmpPath, filePath, StandardCopyOption.REPLACE_EXISTING);
		printLog("Write complete");

		// メモリ上の難易度表情報を新しい内容で置き換える
		var iSp = PlayStyle.SINGLE.ordinal();
		var iDp = PlayStyle.DOUBLE.ordinal();
		var newCollection = new ContentCollection(
				tableDesc,
				lastUpdateDateTime,
				outModifiedDateTimes[iSp], outModifiedDataHashes[iSp],
				outModifiedDateTimes[iDp], outModifiedDataHashes[iDp],
				allContents);
		mCollections.put(tableDesc.getId(), newCollection);
		printLog("UPDATE '%s' complete", tableDesc.getId());
	}

	/**
	 * 読み込み・書き込みの排他処理
	 * @param read 読み込みロックを行うかどうか
	 * @param write 書き込みロックを行うかどうか
	 * @throws IllegalStateException 読み書き排他処理エラーが発生した
	 */
	private void lock(boolean read, boolean write) throws IllegalStateException {
		try {
			if (read) { assertField(mReadLock.lock(), "Failed to lock read"); }
			if (write) { assertField(mWriteLock.lock(), "Failed to write lock"); }
		} catch (IllegalStateException e) {
			mReadLock.unlock();
			mWriteLock.unlock();
			throw e;
		}
	}

	/**
	 * 読み込み・書き込みの排他解除
	 * @param read 読み込みのロック解除を行うかどうか
	 * @param write 書き込みのロック解除を行うかどうか
	 */
	private void unlock(boolean read, boolean write) {
		if (read) { mReadLock.unlock(); }
		if (write) { mWriteLock.unlock(); }
	}

	/**
	 * データ改ざんを検出した時の例外生成処理
	 * @param contentFilePath 難易度表情報ファイルのパス
	 * @param cause 例外の発生原因となる元例外(なければ null で良い)
	 * @throws IOException データ改ざん例外
	 */
	private static void tampering(Path contentFilePath, Throwable cause) throws IOException {
		var msg = String.format("%s: Unacceptable tampering was detected", contentFilePath);
		throw new IOException(msg, cause);
	}
}
