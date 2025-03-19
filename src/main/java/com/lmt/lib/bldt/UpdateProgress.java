package com.lmt.lib.bldt;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 難易度表更新の進捗状況の報告を受け取る進捗報告ハンドラインターフェイスです。
 *
 * <p>当インターフェイスは {@link ContentDatabase#update(HttpClient, Duration, UpdateProgress)} での難易度表更新時、
 * 更新処理の進捗状況の報告を受け取り、ユーザーへの状況報告を行う目的で使用されます。</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface UpdateProgress {
	/**
	 * 報告種別を表す列挙型です。
	 * @since 0.1.0
	 */
	public enum Status {
		/**
		 * 難易度表の更新が開始されたことを表します。
		 * @since 0.1.0
		 */
		START,
		/**
		 * 難易度表の更新が完了し、難易度表データベースが正常に更新されたことを表します。
		 * @since 0.1.0
		 */
		DONE,
		/**
		 * 難易度表に差分がなく、難易度表データベースの更新が不要と判定され、更新処理が完了したことを表します。
		 * @since 0.1.0
		 */
		UNNECESSARY,
		/**
		 * 難易度表の更新中に何らかのエラーが発生したことを表します。
		 * <p>当状態が報告された場合、進捗報告ハンドラの終了後に例外がスローされます。</p>
		 * @since 0.1.0
		 */
		ERROR;
	}

	/**
	 * 何も行わない進捗報告ハンドラです。
	 *
	 * @since 0.1.0
	 */
	public static class Nop implements UpdateProgress {
		/** {@inheritDoc} */
		@Override
		public void publish(TableDescription desc, PlayStyle playStyle, int iDesc, int numDesc, Status status) {
			// Do nothing
		}
	}

	/**
	 * 進捗内容を標準出力に出力する進捗報告ハンドラです。
	 *
	 * @since 0.1.0
	 */
	public static class StdOut implements UpdateProgress {
		/** {@inheritDoc} */
		@Override
		public void publish(TableDescription desc, PlayStyle playStyle, int iDesc, int numDesc, Status status) {
			if (status == Status.START) {
				System.out.printf("(%d/%d) Updating '%s' %s ... ", (iDesc + 1), numDesc, desc.getName(), playStyle);
			} else {
				System.out.println(status);
			}
		}
	}

	/**
	 * {@link Nop} を返します。
	 * @return {@link Nop} オブジェクトのインスタンス
	 * @since 0.1.0
	 */
	public static UpdateProgress nop() {
		return new Nop();
	}

	/**
	 * {@link StdOut} を返します。
	 * @return {@link StdOut} オブジェクトのインスタンス
	 * @since 0.1.0
	 */
	public static UpdateProgress stdout() {
		return new StdOut();
	}

	/**
	 * 難易度表更新の進捗状況の報告を受け取るハンドラメソッドです。
	 * <p>当メソッドの入力パラメータを参照することで難易度表更新処理の進捗状況を把握することができます。
	 * アプリケーションの仕様に応じた進捗報告の振る舞いを当メソッドに記述してください。
	 * 進捗報告の報告種別は {@link Status} を参照してください。</p>
	 * <p>当メソッド内で例外がスローされると難易度表更新処理は中止され、呼び出し元に例外が通知されます。
	 * その場合でも、途中まで保存された難易度表データベースは更新前の状態には戻らず、
	 * 例外がスローされた難易度表とその後更新予定だった難易度表は更新されません。</p>
	 * <p>当メソッドが通知されるのは、各難易度表でのサポート対象のプレースタイルのみです。
	 * サポート外のプレースタイルに対しては進捗報告されないので注意が必要です。</p>
	 * @param desc 更新対象の難易度表定義
	 * @param playStyle 更新対象のプレースタイル
	 * @param iDesc 更新対象の難易度表定義のインデックス値(0オリジン)
	 * @param numDesc 更新対象の難易度表の数
	 * @param status 報告種別
	 * @since 0.1.0
	 */
	void publish(TableDescription desc, PlayStyle playStyle, int iDesc, int numDesc, Status status);
}
