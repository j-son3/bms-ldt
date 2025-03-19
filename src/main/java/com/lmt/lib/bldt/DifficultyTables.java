package com.lmt.lib.bldt;

import static com.lmt.lib.bldt.internal.Assertion.*;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * LDTライブラリが持つ難易度表定義にアクセスするためのプレースホルダクラスです。
 *
 * <p>ライブラリは難易度表定義の情報を内部で保有しており、それらの定義は特に手続きを必要とせず参照することができます。
 * 当ライブラリのユーザーは当クラスの各種静的フィールド・メソッドを通して難易度表の情報にアクセスすることができます。
 * 内部で保有する難易度表の情報については {@link Presets} を参照してください。</p>
 *
 * <p>難易度表定義はユーザー定義により追加することも可能です。追加する場合は {@link TableDescription} を生成し、
 * {@link #add(TableDescription)} を呼び出してください。</p>
 *
 * <p>その他、当クラスではライブラリの全体的な振る舞いを決定する各種操作ができます。
 * 詳細は各種メソッドを参照してください。</p>
 *
 * <p>また、当クラスはエントリポイントを保有しています。Javaの実行環境が整っていればコマンドプロンプト・端末等から
 * 当ライブラリが提供する基本的な機能を実行することができるようになっています。詳細は {@link #main(String[])}
 * を参照してください。</p>
 *
 * @since 0.1.0
 */
@Command(name = "BMS Levelize by Difficulty Tables",
		mixinStandardHelpOptions = true,
		version = "0.1.0",
		description = "Update and Show difficulty tables")
public class DifficultyTables implements Runnable {
	/**
	 * LDTライブラリのバージョン
	 * @since 0.1.0
	 */
	public static final String LIBRARY_VERSION = "0.1.0";
	/**
	 * デフォルトの難易度表データベース格納先パス
	 * @since 0.1.0
	 */
	public static final Path DEFAULT_LOCATION = Path.of(System.getProperty("user.home"), ".com.lmt", "bldt");

	/** 動作モード：難易度表更新 */
	private static final String MODE_UPDATE = "update";
	/** 動作モード：楽曲情報出力 */
	private static final String MODE_SHOW = "show";
	/** 動作モード：難易度表定義出力 */
	private static final String MODE_PRESETS = "presets";
	/** デフォルトの接続・応答タイムアウト */
	private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
	/** 文字列リソースの名前 */
	private static final String BUNDLE_NAME = DifficultyTables.class.getPackageName() + ".string";
	/** ロガー関数 */
	private static Consumer<String> sLogger = null;

	/** 難易度表定義マップ */
	private static Map<String, TableDescription> sTableDescs = Arrays.stream(Presets.values())
			.map(Presets::getTableDescription)
			.collect(Collectors.toMap(td -> td.getId(), td -> td, (td1, td2) -> td1, LinkedHashMap::new));

	/** 動作モード */
	@Parameters(
			index = "0",
			arity = "1",
			paramLabel = "MODE",
			description = "Operation mode. \"update\", \"show\" or \"presets\".")
	private String mMode;
	/** ID */
	@Option(names = { "-t", "--target-id" },
			defaultValue = "",
			paramLabel = "ID",
			description = "Difficulty table ID to process. If omitted, all preset difficulty tables will be processed.")
	private String mId;
	/** 難易度表データベース格納先パス */
	@Option(names = { "-l", "--location" },
			defaultValue = "",
					paramLabel = "LOCATION",
			description = "Database location. If omitted, the default location will be used.")
	private String mLocation;
	/** デバッグモードかどうか */
	@Option(names = "--debug",
			hidden = true,
			description = "Enable debug mode. This option is hidden.")
	private boolean mIsDebugMode;

	/** 静的イニシャライザ */
	static {
		setLocale(Locale.getDefault());
	}

	/**
	 * 全ての登録済み難易度表定義のストリームを返します。
	 * <p>デフォルトでは {@link Presets} で定義された全ての難易度表定義が走査されます。
	 * {@link #add(TableDescription)} で難易度表定義を追加すると、その定義も走査されます。</p>
	 * @return 全ての登録済み難易度表定義のストリーム
	 * @since 0.1.0
	 */
	public static Stream<TableDescription> all() {
		return sTableDescs.values().stream();
	}

	/**
	 * 指定したIDの難易度表定義を取得します。
	 * @param id ID
	 * @return 指定したIDの難易度表定義。該当する難易度表定義が存在しない場合はnull。
	 * @throws NullPointerException idがnull
	 * @since 0.1.0
	 */
	public static TableDescription get(String id) {
		assertArgNotNull(id, "id");
		return sTableDescs.get(id);
	}

	/**
	 * 難易度表定義を追加します。
	 * <p>当メソッドはライブラリが内部で保有する難易度表定義以外の難易度表を追加したい場合に使用します。
	 * 追加された難易度表定義の楽曲情報は、難易度表データベースの更新対象になります。</p>
	 * <p>追加する難易度表定義のIDが他と競合しないように注意してください。競合すると例外がスローされます。</p>
	 * <p>当メソッドはアプリケーションの起動直後の初期化処理時に実行され、アプリケーションにとって必要な難易度表定義を
	 * 追加することを想定して設計されています。一度追加された難易度表定義を削除する機能は用意されておらず、
	 * アプリケーションのプロセスが終了するまで残り続けることに注意してください。</p>
	 * @param tableDesc 追加する難易度表定義
	 * @throws NullPointerException tableDescがnull
	 * @throws IllegalArgumentException IDが競合している
	 * @since 0.1.0
	 */
	public static void add(TableDescription tableDesc) {
		assertArgNotNull(tableDesc, "td");
		assertArg(!sTableDescs.containsKey(tableDesc.getId()), "ID '%s' is already exists", tableDesc.getId());
		sTableDescs.put(tableDesc.getId(), tableDesc);
	}

	/**
	 * デバッグログを出力します。
	 * <p>当メソッドで出力したデバッグログは {@link #setLogger(Consumer)} でロガー関数が登録されている時に出力されます。
	 * デバッグログは当ライブラリの内部動作および {@link Parser} を実装したパーサの内部動作を確認する目的で使用します。
	 * アプリケーション固有の出力処理を行うために当メソッドを使用することは非推奨です。</p>
	 * @param msg メッセージ
	 * @since 0.1.0
	 */
	public static void printLog(String msg) {
		if (Objects.nonNull(sLogger)) {
			var now = LocalDateTime.now();
			sLogger.accept(String.format("%s [DEBUG] %s", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), msg));
		}
	}

	/**
	 * デバッグログを出力します。
	 * <p>当メソッドはメッセージを printf() の書式に従って出力メッセージを構築する点を除き {@link #printLog(String)}
	 * と同等の動作になります。</p>
	 * @param format メッセージの書式
	 * @param args メッセージの引数リスト
	 * @throws IllegalFormatException メッセージの書式が不正
	 * @since 0.1.0
	 */
	public static void printLog(String format, Object...args) {
		if (Objects.nonNull(sLogger)) {
			printLog(String.format(format, args));
		}
	}

	/**
	 * ロガーを設定します。
	 * <p>当メソッドで設定したロガーは {@link #printLog(String)} 等でデバッグログを出力する際に使用されます。
	 * デフォルトではロガーは設定されておらず、デバッグログは出力されません。</p>
	 * <p>ロガーを取り除きたい場合は null を指定してください。</p>
	 * @param logger ロガー
	 * @since 0.1.0
	 */
	public static void setLogger(Consumer<String> logger) {
		sLogger = logger;
	}

	/**
	 * LDTライブラリが使用するロケールを設定します。
	 * <p>ロケールは難易度表名称等の各種文字列表現を決定する際に参照されます。当メソッドが実行されない場合、
	 * システムのデフォルトロケールが使用されます。アプリケーションで高度な言語設定を使用しない限り、
	 * 特に呼び出す必要はありません。</p>
	 * @param locale ロケール
	 * @throws NullPointerException localeがnull
	 * @since 0.1.0
	 */
	public static void setLocale(Locale locale) {
		assertArgNotNull(locale, "locale");
		var bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
		for (var tableDesc : sTableDescs.values()) {
			var name = bundle.getString(tableDesc.getId() + ".name");
			tableDesc.setName(name);
		}
	}

	/**
	 * LDTライブラリのアプリケーションエントリポイントです。
	 * <p>当ライブラリでは、難易度表に関する基本的な機能を操作するための CLI(コマンドラインインターフェイス)
	 * をサポートします。これを利用することで、当ライブラリを使用したアプリケーションを使用せずに難易度表の操作を
	 * 行うことができます。</p>
	 * <p>CLIがサポートする機能</p>
	 * <ul>
	 * <li>update: {@link Presets} に定義されたプリセット難易度表の更新機能(インターネット接続が必要です)</li>
	 * <li>show: {@link Presets} に定義されたプリセット難易度表の楽曲情報一覧出力</li>
	 * <li>presets: {@link Presets} に定義されたプリセット難易度表の定義内容一覧出力</li>
	 * </ul>
	 * <p>CLI機能の動作オプションは以下の通りです。</p>
	 * <p><strong>-l, --location</strong></p>
	 * <p>難易度表更新・楽曲情報一覧出力時、処理対象となる難易度表データベースのパスを指定します。
	 * 省略時は当ライブラリ既定の難易度表データベース格納先パスが使用されます。
	 * 難易度表更新時、指定されたパスのディレクトリが存在しない場合はディレクトリが作成されます。</p>
	 * <p><strong>-t, --target-id</strong></p>
	 * <p>処理対象となる難易度表定義のIDを1個指定します。複数指定はできません。
	 * 省略時は全ての難易度表定義が処理対象となります(デフォルトの動作)。不明なIDを指定するとエラーになります。</p>
	 * <p>以下にCLI実行コマンド例を記載します。ライブラリのファイル名・パスは実際の格納場所で読み替えてください。</p>
	 * <pre>
	 * LDTライブラリのバージョンを表示する
	 * java -jar bms-ldt-x.x.x.jar --version
	 *
	 * CLIのヘルプを表示する
	 * java -jar bms-ldt-x.x.x.jar --help
	 *
	 * 難易度表を全て更新する
	 * java -jar bms-ldt-x.x.x.jar update
	 *
	 * 指定したディレクトリの難易度表データベースに特定の難易度表をダウンロード・更新する
	 * java -jar bms-ldt-x.x.x.jar update -l C:\Users\john\bldt -t genocide_i
	 *
	 * 指定した難易度表の楽曲情報一覧を出力する
	 * java -jar bms-ldt-x.x.x.jar show -t satellite
	 *
	 * 難易度表の定義内容を出力する
	 * java -jar bms-ldt-x.x.x.jar presets</pre>
	 * @param args コマンドライン引数
	 * @since 0.1.0
	 */
	public static void main(String[] args) {
		var exitCode = new CommandLine(new DifficultyTables()).execute(args);
        System.exit(exitCode);
	}

	/**
	 * CLIのメイン処理
	 * @hidden
	 */
	@Override
	public void run() {
		try {
			// ロガーの設定
			// デバッグモード時は内部ログを標準出力で出力する
			setLogger(mIsDebugMode ? System.out::println : null);

			// モードによる処理の分岐
			if (mMode.equals(MODE_UPDATE)) {
				// 難易度表更新モード
				verifyLocation();
				verifyId();
				update();
				System.exit(0);
			} else if (mMode.equals(MODE_SHOW)) {
				// 難易度表出力モード
				verifyLocation();
				verifyId();
				show();
				System.exit(0);
			} else if (mMode.equals(MODE_PRESETS)) {
				// プリセット難易度表定義出力モード
				presets();
				System.exit(0);
			} else {
				// 不明なモード
				System.out.println(mMode + ": Unknown mode.");
				System.exit(-1);
			}
		} catch (Exception e) {
			// 想定外の例外がスローされた場合はスタックトレースを出力する
			System.out.println("********** ERROR **********");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * DB格納先フォルダのチェック
	 */
	private void verifyLocation() {
		// DB格納先フォルダが未指定の場合は規定のフォルダを指定する
		if (mLocation.isEmpty()) {
			mLocation = DEFAULT_LOCATION.toString();
		}
	}

	/**
	 * IDのチェック
	 */
	private void verifyId() {
		// 指定されたIDの難易度表が存在するか確認する
		if (!mId.isEmpty() && !sTableDescs.containsKey(mId)) {
			System.out.printf("'%s': There is no difficulty table with such an ID.\n", mId);
			System.exit(-1);
		}
	}

	/**
	 * 難易度表更新処理
	 * @throws Exception 何らかのエラーが発生した
	 */
	private void update() throws Exception {
		// HTTPクライアントを生成する
		// 接続タイムアウト、プロキシの設定はデフォルトのものを使用する
		var httpClient = HttpClient.newBuilder()
				.connectTimeout(DEFAULT_TIMEOUT)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.proxy(ProxySelector.getDefault())
				.build();

		// 難易度表データベースの更新処理
		var db = new ContentDatabase(Path.of(mLocation), true);
		if (mId.isEmpty()) {
			// 全ての難易度表を更新する
			System.out.println("Update all preset difficulty tables.");
			db.update(httpClient, DEFAULT_TIMEOUT, UpdateProgress.stdout());
		} else {
			// 指定されたIDの難易度表を更新する
			db.update(httpClient, mId, DEFAULT_TIMEOUT, UpdateProgress.stdout());
		}

		System.out.println("Completed");
	}

	/**
	 * 楽曲情報一覧出力処理
	 * @throws Exception 何らかのエラーが発生した
	 */
	private void show() throws Exception {
		// 一覧のヘッダ部を出力する
		System.out.println("Table Name\tLevel\tTitle\tArtist\tStyle\tBody URL\tAdditional URL\tMD5\tSHA-256");

		// 全楽曲情報の出力処理
		var db = new ContentDatabase(Path.of(mLocation), false);
		if (mId.isEmpty()) {
			// 全ての難易度表の全楽曲情報を出力する
			db.all().forEach(this::show);
		} else {
			// 指定されたIDの難易度表の全楽曲情報を出力する
			show(db.get(mId));
		}
	}

	/**
	 * 指定された楽曲情報一覧出力処理
	 * @param collection 楽曲情報一覧
	 */
	private void show(ContentCollection collection) {
		// 楽曲情報を並び替える
		// SP->DPの順で、タイトル・アーティストを昇順ソートする
		var contents = collection.all().sorted((c1, c2) -> {
			var n = Integer.compare(c1.getPlayStyle().ordinal(), c2.getPlayStyle().ordinal());
			n = (n == 0) ? c1.getTitle().compareToIgnoreCase(c2.getTitle()) : n;
			n = (n == 0) ? c1.getArtist().compareToIgnoreCase(c2.getArtist()) : n;
			return n;
		}).collect(Collectors.toList());

		// 全楽曲情報を出力する
		var tableDesc = collection.getTableDescription();
		var tableName = strNormalize(tableDesc.getName());
		for (var content : contents) {
			var styleDesc = tableDesc.getPlayStyleDescription(content.getPlayStyle());
			var symbol = styleDesc.getSymbol();
			var label = styleDesc.getLabels().get(content.getLevelIndex());
			System.out.printf("%s\t%s%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n",
					tableName,
					strNormalize(symbol),
					strNormalize(label),
					strNormalize(content.getTitle()),
					strNormalize(content.getArtist()),
					content.getPlayStyle(),
					strNormalize(content.getBodyUrl()),
					strNormalize(content.getAdditionalUrl()),
					content.getMd5(),
					content.getSha256());
		}
	}

	/**
	 * 難易度表定義出力処理
	 * @throws Exception 何らかのエラーが発生した
	 */
	private void presets() throws Exception {
		// 全プリセットの情報を出力する
		var presets = all().collect(Collectors.toList());
		for (var tableDesc : presets) {
			System.out.printf("===== %s =====\n", tableDesc.getName());
			System.out.printf("ID : %s\n", tableDesc.getId());
			System.out.printf("URL: %s\n", tableDesc.getOfficialUrl());
			System.out.printf("SP : %s\n", Objects.nonNull(tableDesc.getSingleDescription()) ? "Enable" : "Disable");
			System.out.printf("DP : %s\n", Objects.nonNull(tableDesc.getDoubleDescription()) ? "Enable" : "Disable");
			System.out.println();
		}
	}

	/**
	 * 出力文字列調整
	 * @param str 調整対象文字列
	 * @return 調整後の文字列
	 */
	private static String strNormalize(Object str) {
		// nullの場合はnullのまま返す
		if (Objects.isNull(str)) {
			return null;
		}

		// タブ文字はセパレータとして使用しているので、文字列にタブが含まれる場合は空白に変換する
		var replaced = str.toString().replace('\t', ' ');
		return replaced;
	}

	/**
	 * コンストラクタ
	 */
	private DifficultyTables() {
		// Do nothing
	}
}
