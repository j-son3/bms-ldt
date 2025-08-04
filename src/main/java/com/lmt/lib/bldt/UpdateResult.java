package com.lmt.lib.bldt;

/**
 * 難易度表データベースの更新結果を表すクラスです。
 *
 * @since 0.2.0
 */
public class UpdateResult {
	/** 更新結果 */
	private Type mType;
	/** 失敗時、キャッチした例外 */
	private Throwable mCause;

	/** 難易度表データベース更新結果種別 */
	public enum Type {
		/** 成功 */
		SUCCESS,
		/** 中止 */
		ABORT,
		/** 失敗 */
		ERROR;
	}

	/**
	 * コンストラクタ
	 * @param type 更新結果種別
	 */
	UpdateResult(Type type) {
		mType = type;
		mCause = null;
	}

	/**
	 * コンストラクタ(失敗時用)
	 * @param cause キャッチした例外
	 */
	UpdateResult(Throwable cause) {
		mType = Type.ERROR;
		mCause = cause;
	}

	/**
	 * 更新結果種別を取得します。
	 * @return 更新結果種別
	 * @since 0.2.0
	 */
	public Type getType() {
		return mType;
	}

	/**
	 * 失敗時、キャッチした例外を取得します。
	 * <p>失敗以外の結果では null が返ります。</p>
	 * @return キャッチした例外
	 * @since 0.2.0
	 */
	public Throwable getCause() {
		return mCause;
	}

	/**
	 * 更新処理が成功したかどうかを取得します。
	 * @return 成功時、true
	 * @since 0.2.0
	 */
	public boolean isSuccess() {
		return mType == Type.SUCCESS;
	}
}
