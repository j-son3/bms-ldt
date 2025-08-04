package com.lmt.lib.bldt;

import static com.lmt.lib.bldt.internal.Assertion.*;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.lmt.lib.bldt.internal.Utility;

/**
 * 難易度表情報を表すクラスです。
 *
 * <p>難易度表情報とは、1個の難易度表の定義内容、更新日時等の情報、および楽曲情報のリストをまとめたものです。
 * 当クラスは1個の難易度表情報ファイルと1対1の関係にあり、難易度表情報ファイルをメモリ上に展開した時の情報と等価です。</p>
 *
 * @since 0.1.0
 */
public class ContentCollection {
	/** 難易度表定義 */
	private TableDescription mTableDesc;
	/** 難易度表情報の最終更新日時 */
	private ZonedDateTime mLastUpdateDateTime;
	/** 楽曲情報元データの最終更新日時 */
	private ZonedDateTime[] mModifiedDateTimes = new ZonedDateTime[PlayStyle.COUNT];
	/** 楽曲情報の元データのハッシュ値 */
	private String[] mModifiedDataHashes = new String[PlayStyle.COUNT];
	/** 楽曲情報リスト */
	private List<ContentDescription> mContents;
	/** タイトル・アーティストによる楽曲情報マップ */
	private Map<String, ContentDescription> mMappedMeta = new HashMap<>();
	/** MD5による楽曲情報マップ */
	private Map<String, ContentDescription> mMappedMd5 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	/** SHA-256による楽曲情報マップ */
	private Map<String, ContentDescription> mMappedSha256 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	/**
	 * 新しい難易度表情報オブジェクトを構築します。
	 * <p>当クラスのオブジェクト構築は当ライブラリのユーザーが直接行うことを想定していません。
	 * 難易度表情報の管理・更新は {@link ContentDatabase} を利用して間接的に行うことを推奨します。</p>
	 * @param tableDesc 難易度表定義
	 * @param lastUpdateDateTime 難易度表情報の最終更新日時
	 * @param spModifiedDateTime シングルプレーの楽曲情報元データの最終更新日時
	 * @param spModifiedDataHash シングルプレーの楽曲情報元データのハッシュ値
	 * @param dpModifiedDateTime ダブルプレーの楽曲情報元データの最終更新日時
	 * @param dpModifiedDataHash ダブルプレーの楽曲情報元データのハッシュ値
	 * @param contents 楽曲情報リスト
	 * @throws NullPointerException tableDesc が null
	 * @throws NullPointerException contents が null
	 * @throws IllegalArgumentException spModifiedDataHash が文字列のSHA-256の形式ではない
	 * @throws IllegalArgumentException dpModifiedDataHash が文字列のSHA-256の形式ではない
	 * @since 0.1.0
	 */
	public ContentCollection(TableDescription tableDesc, ZonedDateTime lastUpdateDateTime,
			ZonedDateTime spModifiedDateTime, String spModifiedDataHash,
			ZonedDateTime dpModifiedDateTime, String dpModifiedDataHash, Collection<ContentDescription> contents) {
		assertArgNotNull(tableDesc, "tableDesc");
		assertArg(Objects.isNull(spModifiedDataHash) || Utility.isSha256(spModifiedDataHash),
				"'spModifiedDataHash' is not valid: %s", spModifiedDataHash);
		assertArg(Objects.isNull(dpModifiedDataHash) || Utility.isSha256(dpModifiedDataHash),
				"'dpModifiedDataHash' is not valid: %s", dpModifiedDataHash);
		assertArgNotNull(contents, "contents");
		mTableDesc = tableDesc;
		mLastUpdateDateTime = lastUpdateDateTime;
		mModifiedDateTimes[PlayStyle.SINGLE.ordinal()] = spModifiedDateTime;
		mModifiedDateTimes[PlayStyle.DOUBLE.ordinal()] = dpModifiedDateTime;
		mModifiedDataHashes[PlayStyle.SINGLE.ordinal()] = Utility.normalizeHash(spModifiedDataHash);
		mModifiedDataHashes[PlayStyle.DOUBLE.ordinal()] = Utility.normalizeHash(dpModifiedDataHash);
		mContents = List.copyOf(contents);
		for (var c : mContents) {
			mMappedMeta.put(metaKey(c.getTitle(), c.getArtist(), c.getPlayStyle()), c);
			if (Objects.nonNull(c.getMd5())) { mMappedMd5.put(c.getMd5(), c); }
			if (Objects.nonNull(c.getSha256())) { mMappedSha256.put(c.getSha256(), c); }
		}
	}

	/**
	 * 難易度表定義を取得します。
	 * @return 難易度表定義
	 * @since 0.1.0
	 */
	public TableDescription getTableDescription() {
		return mTableDesc;
	}

	/**
	 * 難易度表情報の最終更新日時を取得します。
	 * @return 難易度表情報の最終更新日時
	 */
	public ZonedDateTime getLastUpdateDateTime() {
		return mLastUpdateDateTime;
	}

	/**
	 * 指定したプレースタイルの楽曲情報元データの最終更新日時を取得します。
	 * <p>以下の条件のいずれかを満たす場合、null を返します。</p>
	 * <ul>
	 * <li>指定プレースタイルが当該難易度表で非サポート</li>
	 * <li>当該難易度表で一度も難易度表情報の更新が行われていない</li>
	 * </ul>
	 * @param playStyle プレースタイル
	 * @return 楽曲情報元データの最終更新日時、または null
	 * @throws NullPointerException playStyle が null
	 * @since 0.1.0
	 */
	public ZonedDateTime getModifiedDateTime(PlayStyle playStyle) {
		assertArgNotNull(playStyle, "playStyle");
		return mModifiedDateTimes[playStyle.ordinal()];
	}

	/**
	 * 指定したプレースタイルの楽曲情報元データのハッシュ値を取得します。
	 * <p>以下の条件のいずれかを満たす場合、null を返します。</p>
	 * <ul>
	 * <li>指定プレースタイルが当該難易度表で非サポート</li>
	 * <li>当該難易度表で一度も難易度表情報の更新が行われていない</li>
	 * </ul>
	 * @param playStyle プレースタイル
	 * @return 楽曲情報元データのハッシュ値、または null
	 * @throws NullPointerException playStyle が null
	 * @since 0.1.0
	 */
	public String getModifiedDataHash(PlayStyle playStyle) {
		assertArgNotNull(playStyle, "playStyle");
		return mModifiedDataHashes[playStyle.ordinal()];
	}

	/**
	 * 楽曲情報の数を取得します。
	 * @return 楽曲情報の数
	 * @since 0.1.0
	 */
	public int getCount() {
		return mContents.size();
	}

	/**
	 * 全ての楽曲情報のストリームを返します。
	 * @return 楽曲情報のストリーム
	 * @since 0.1.0
	 */
	public Stream<ContentDescription> all() {
		return mContents.stream();
	}

	/**
	 * 楽曲情報を取得します。
	 * @param index インデックス値
	 * @return 楽曲情報
	 * @throws IndexOutOfBoundsException index が0未満または {@link #getCount()} 以上
	 * @since 0.1.0
	 */
	public ContentDescription get(int index) {
		assertArgIndexRange(index, mContents.size(), "index");
		return mContents.get(index);
	}

	/**
	 * この難易度表情報から指定した条件に該当する1件の楽曲情報を検索します。
	 * <p>当メソッドでは指定した条件を用いた完全一致検索を行います。検索は最も信頼できる情報を優先的に使用し、
	 * その順番は SHA-256, MD5, タイトル＆アーティスト＆プレースタイル の順になっています。ただし、SHA-256, MD5
	 * は任意情報であり、楽曲情報にそれらが未登録の場合は検索でヒットしません。
	 * タイトル＆アーティスト＆プレースタイルは最も競合する可能性がある情報ですが、
	 * 全ての楽曲情報が必ず保有している情報で、確実に検索条件として照合が行われます。</p>
	 * <p>上記の理由から SHA-256, MD5 は指定省略可能で、タイトル＆アーティスト＆プレースタイルは必須となります。</p>
	 * <p>正規表現やその他の検索条件を使用して複雑な検索を行いたい場合は {@link #all()} を使用してください。
	 * 当メソッドで抽出可能な楽曲情報は1件のみです。</p>
	 * @param title タイトル
	 * @param artist アーティスト
	 * @param playStyle プレースタイル
	 * @param md5 MD5 または null
	 * @param sha256 SHA-256 または null
	 * @return 検索条件に該当する楽曲情報。該当なしの場合は null。
	 * @throws NullPointerException title が null
	 * @throws NullPointerException artist が null
	 * @throws NullPointerException playStyle が null
	 * @since 0.1.0
	 */
	public ContentDescription query(String title, String artist, PlayStyle playStyle,
			String md5, String sha256) {
		assertArgNotNull(title, "title");
		assertArgNotNull(artist, "artist");
		assertArgNotNull(playStyle, "playStyle");

		// SHA-256指定があり、SHA-256が定義されている場合、SHA-256による照合を試みる
		var c = (ContentDescription)null;
		if (Objects.nonNull(sha256) && Objects.nonNull(c = mMappedSha256.get(sha256))) {
			return c;
		}

		// MD5指定があり、MD5が定義されている場合、MD5による照合を試みる
		if (Objects.nonNull(md5) && Objects.nonNull(c = mMappedMd5.get(md5))) {
			return c;
		}

		// タイトル・アーティストによる照合を試みる
		return mMappedMeta.get(metaKey(title, artist, playStyle));
	}

	/**
	 * タイトル＆アーティスト検索キー生成
	 * @param title タイトル
	 * @param artist アーティスト
	 * @param playStyle プレースタイル
	 * @return タイトル＆アーティスト検索キー
	 */
	private static String metaKey(String title, String artist, PlayStyle playStyle) {
		return new StringBuilder()
				.append("t=").append(title)
				.append(",a=").append(artist)
				.append(",s=").append(playStyle.shortName)
				.toString();
	}
}
