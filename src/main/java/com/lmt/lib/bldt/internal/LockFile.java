package com.lmt.lib.bldt.internal;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * ファイルロッククラス
 *
 * プロセス間で排他制御を行うにあたり、ファイルロックの仕組みを利用する。
 * 当クラスは1クラスあたり1ファイルのロックを表す。
 *
 * @hidden
 */
public class LockFile {
	/** ロックファイルパス */
	private Path mLockFilePath = null;
	/** ロックファイルチャンネル */
	private FileChannel mFileChannel = null;
	/** ファイルロックオブジェクト */
	private FileLock mFileLock = null;

	/**
	 * コンストラクタ
	 * @param lockFilePath ロックファイルパス
	 */
	public LockFile(Path lockFilePath) {
		mLockFilePath = lockFilePath;
	}

	/**
	 * ファイルロックを実施
	 * @return ファイルロックに成功するとtrue、失敗時はfalse。
	 */
	public boolean lock() {
		// ロック済みの場合はロック不可
		if (isLocked()) {
			return false;
		}

		// ロックファイルを開く
		var fileChannel = (FileChannel)null;
		try {
			// ロックファイルは無ければ作成する
			fileChannel = FileChannel.open(mLockFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		} catch (IOException e) {
			// ロックファイルが作成・オープンできない場合はロック失敗とする
			return false;
		}

		// ファイルロックを行う
		var fileLock = (FileLock)null;
		try  {
			// ファイルロックを試行してOKならロック成功とする
			fileLock = fileChannel.tryLock();
			if (fileLock == null) {
				return false;
			}
		} catch (IOException e) {
			// ロック中のIOエラーはロック失敗と見なす
			return false;
		} finally {
			// ロック失敗時は全て閉じる
			if (fileLock == null) {
				try { fileChannel.close(); } catch (IOException e) {}
			}
		}

		// ロックしたファイルの情報を保持しておく
		mFileChannel = fileChannel;
		mFileLock = fileLock;

		return true;
	}

	/**
	 * ファイルロックを解除
	 * @return ロック解除に成功するとtrue、失敗時はfalse。
	 */
	public boolean unlock() {
		// ロック済みの場合はアンロック不可
		if (!isLocked()) {
			return false;
		}

		// ロックを解除する
		try {
			mFileLock.close();
		} catch (IOException e) {
			// Do nothing
		}

		// ロックファイルを閉じる
		try {
			mFileChannel.close();
		} catch (IOException e) {
			// Do nothing
		}

		// ロックファイルを解放する
		mFileChannel = null;
		mFileLock = null;

		return true;
	}

	/**
	 * ファイルがロック中かどうかを判定
	 * @return ファイルがロック中の場合true
	 */
	public boolean isLocked() {
		return (mFileLock != null);
	}

	/**
	 * ファイルがロック可能であるかどうかを判定
	 * @return ファイルがロック可能であればtrue
	 */
	public boolean test() {
		// ロックしてみてその結果を返す
		boolean lockable = lock();
		if (lockable) { unlock(); }
		return lockable;
	}
}
