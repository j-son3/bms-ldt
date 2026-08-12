package com.lmt.lib.bldt;

import java.awt.Window;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * 難易度表ツール・難易度表更新画面の動作オプションを指定するI/Fです。
 *
 * <p>LDTライブラリが提供する難易度表ツールの機能をカスタマイズするために使用されます。
 * 当I/Fの全てのメソッドは規定の動作が定義されています。全ての動作を規定の動作としたい場合は
 * {@link #DEFAULT} を指定することができます。</p>
 *
 * <p>当I/Fの各メソッドは全てAWT EventQueueのディスパッチスレッドから実行されます。
 * 特に理由がない限り各メソッドの処理は直ちに完了させるようにしてください。
 * そうしないと、OSによって「応答なしのアプリケーション」として判定される可能性があります。</p>
 *
 * @see DifficultyTables#guiBrowse(Window, ContentDatabase, GuiOption)
 * @see DifficultyTables#guiUpdate(Window, ContentDatabase, String, GuiOption)
 *
 * @since 0.4.0
 */
public interface GuiOption {
	/** 全ての動作をデフォルトの動作とする場合の動作オプション */
	public static final GuiOption DEFAULT = new GuiOption() {};

	/**
	 * 難易度表ツール上から、選択された難易度表の公式サイトへジャンプする機能を有効にするかどうかを決定します。
	 * <p>この機能を有効にすると難易度表ツールに「公式サイトボタン」が追加されます。
	 * そのボタンをクリックすると選択中の難易度表の公式サイトを「規定のWebブラウザ」で表示します。</p>
	 * <p>難易度表更新画面ではこの値は参照されません。</p>
	 * <p>デフォルトではこの機能は「無効」に設定されています。</p>
	 * @return true:有効, false:無効
	 */
	default boolean supportOfficialSite() {
		return false;
	}

	/**
	 * 難易度表ツール上から、選択された難易度表の更新処理をトリガーする機能を有効にするかどうかを決定します。
	 * <p>この機能を有効にすると難易度表ツールに「更新ボタン」が追加されます。
	 * そのボタンをクリックすると難易度表更新画面が表示され、更新対象の難易度表の更新が開始されます。</p>
	 * <p>難易度表更新画面ではこの値は参照されません。</p>
	 * <p>デフォルトではこの機能は「無効」に設定されています。</p>
	 * @return true:有効, false:無効
	 */
	default boolean supportUpdate() {
		return false;
	}

	/**
	 * 難易度表更新画面から、更新結果ログを表示する機能を有効にするかどうかを決定します。
	 * <p>この機能を有効にすると難易度表更新画面に「ログ表示ボタン」が追加されます。
	 * そのボタンをクリックすると {@link #showLog(Window)} が呼び出されますので、
	 * そのタイミングで直前に {@link #openLog()} で開いた更新結果ログを開く処理を実行してください。</p>
	 * <p>デフォルトではこの機能は「無効」に設定されています。</p>
	 * @return true:有効, false:無効
	 */
	default boolean supportShowLog() {
		return false;
	}

	/**
	 * 更新結果ログを開きます。
	 * <p>当メソッドが呼ばれたら難易度表更新画面での難易度表更新の結果を記録するための出力ストリームを返してください。
	 * 出力ストリームの内容は任意です(例：System.out)。nullを返すと更新結果ログは出力されません。</p>
	 * <p>画面に出力される更新結果ログは更新件数が多いと省略表示されますが、返された出力ストリームへは
	 * 全てのログを出力しようとします。</p>
	 * <p>デフォルトではnullを返します。</p>
	 * @return 更新結果ログを出力するストリーム
	 * @throws IOException 更新結果ログのオープンに失敗した
	 */
	default OutputStream openLog() throws IOException {
		return null;
	}

	/**
	 * 更新結果ログを閉じます。
	 * <p>難易度表更新処理が完了し、更新結果ログの出力が全て行われた後で実行されます。
	 * {@link #supportShowLog()} でfalseを返したり、{@link #openLog()} でnullを返していた場合、
	 * 当メソッドは実行されません。</p>
	 * <p>デフォルトでは更新結果ログのストリームに対して close() を呼び出す実装になっています。
	 * System.out等、close()を実行したくない場合は当メソッドをオーバーライドし、然るべき実装を行ってください。</p>
	 * @param logStream {@link #openLog()} で開いた出力ストリーム
	 * @throws IOException 更新結果ログのクローズに失敗した
	 */
	default void closeLog(OutputStream logStream) throws IOException {
		logStream.close();
	}

	/**
	 * 更新結果ログを表示します。
	 * <p>当メソッドは難易度表更新画面の「ログ表示ボタン」をクリックした時に実行されます。
	 * 当メソッドが実行された時に行うのは、「直前の更新結果をログ表示する」ことです。
	 * ログ表示を画面上に表示する場合は引数の dialog を親ウィンドウとし、モーダルダイアログで表示してください。
	 * 外部アプリで表示したい場合は直前の {@link #openLog()} で開いた更新結果ログを指定するようにしてください。</p>
	 * <p>デフォルト実装では何も行われません。更新結果ログ表示をサポートする場合は実装必須です。</p>
	 * @param dialog 難易度表更新画面のウィンドウを表すオブジェクト
	 * @throws IOException 更新結果ログの表示に失敗した
	 */
	default void showLog(Window dialog) throws IOException {
		// Do nothing
	}

	/**
	 * 難易度表更新結果の通知です。
	 * <p>当メソッドは難易度表更新画面で処理対象難易度表の更新処理が全て完了したタイミングで実行されます。
	 * 更新結果は、処理対象となった難易度表分が格納されています。キーは難易度表IDです。</p>
	 * <p>デフォルト実装では何も行われません。</p>
	 * @param results 更新処理の結果を格納するマップ
	 */
	default void updateResult(Map<String, UpdateResult> results) {
		// Do nothing
	}
}
