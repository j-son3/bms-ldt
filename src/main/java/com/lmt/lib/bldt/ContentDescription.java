package com.lmt.lib.bldt;

import static com.lmt.lib.bldt.internal.Assertion.*;

import java.net.URL;
import java.util.Objects;

import com.lmt.lib.bldt.internal.Utility;

/**
 * 1個の楽曲情報を表すクラスです。
 *
 * <p>難易度表の楽曲情報として必須の情報は、楽曲のタイトル・アーティスト、および当該難易度表での難易度となります。
 * 当クラスではそれ以外にも対象楽曲の入手に必要な情報や検索に役立つ任意情報を保有します。
 * これらの情報を保有するかどうかは難易度表、または楽曲ごとに異なります。</p>
 *
 * @since 0.1.0
 */
public class ContentDescription {
	/** タイトル */
	private String mTitle;
	/** アーティスト */
	private String mArtist;
	/** プレースタイル */
	private PlayStyle mPlayStyle;
	/** 難易度インデックス */
	private int mLevelIndex;
	/** 楽曲本体入手先URL */
	private URL mBodyUrl;
	/** 差分譜面入手先URL */
	private URL mAdditionalUrl;
	/** MD5 */
	private String mMd5;
	/** SHA-256 */
	private String mSha256;

	/**
	 * 新しい楽曲情報オブジェクトを構築します。
	 * <p>当クラスのオブジェクト構築は当ライブラリのユーザーが直接行うことを想定していません。
	 * 楽曲情報の管理・更新は {@link ContentDatabase} を利用して間接的に行うことを推奨します。</p>
	 * @param title タイトル
	 * @param artist アーティスト
	 * @param playStyle プレースタイル
	 * @param levelIndex 難易度インデックス、または null
	 * @param bodyUrl 楽曲本体入手先URL、または null
	 * @param additionalUrl 差分譜面入手先URL、または null
	 * @param md5 MD5、または null
	 * @param sha256 SHA-256、または null
	 * @throws NullPointerException title が null
	 * @throws NullPointerException artist が null
	 * @throws NullPointerException playStyle が null
	 * @throws IllegalArgumentException title が空文字列
	 * @throws IllegalArgumentException levelIndex が負の値
	 * @throws IllegalArgumentException md5 がMD5の形式ではない
	 * @throws IllegalArgumentException sha256 がSHA-256の形式ではない
	 * @since 0.1.0
	 */
	public ContentDescription(String title, String artist, PlayStyle playStyle, int levelIndex, URL bodyUrl,
			URL additionalUrl, String md5, String sha256) {
		assertArgNotNull(title, "title");
		assertArgNotNull(artist, "artist");
		assertArgNotNull(playStyle, "playStyle");
		assertArg(!title.isEmpty(), "Can't specify empty to title");
		assertArg(levelIndex >= 0, "levelIndex is negative: %d", levelIndex);
		assertArg(Objects.isNull(md5) || Utility.isMd5(md5), "Invalid MD5: %s", md5);
		assertArg(Objects.isNull(sha256) || Utility.isSha256(sha256), "Invalid SHA256: %s", sha256);
		mTitle = title;
		mArtist = artist;
		mPlayStyle = playStyle;
		mLevelIndex = levelIndex;
		mBodyUrl = bodyUrl;
		mAdditionalUrl = additionalUrl;
		mMd5 = Utility.normalizeHash(md5);
		mSha256 = Utility.normalizeHash(sha256);
	}

	/**
	 * タイトルを取得します。
	 * <p>この値は #TITLE の値がそのまま格納されていることが期待されます。</p>
	 * @return タイトル
	 * @since 0.1.0
	 */
	public String getTitle() {
		return mTitle;
	}

	/**
	 * アーティストを取得します。
	 * <p>この値は #ARTIST の値がそのまま格納されていることが期待されます。</p>
	 * @return アーティスト
	 * @since 0.1.0
	 */
	public String getArtist() {
		return mArtist;
	}

	/**
	 * プレースタイルを取得します。
	 * @return プレースタイル
	 * @since 0.1.0
	 */
	public PlayStyle getPlayStyle() {
		return mPlayStyle;
	}

	/**
	 * 難易度インデックスを取得します。
	 * <p>この値は {@link PlayStyleDescription#getLabels()} が返すリストのインデックス値として使用する想定する値です。</p>
	 * @return 難易度インデックス
	 * @since 0.1.0
	 */
	public int getLevelIndex() {
		return mLevelIndex;
	}

	/**
	 * 楽曲本体入手先URLを取得します。
	 * <p>このURLは当該楽曲本体、つまりBMSファイルと音声ファイル、動画ファイル等がパッケージングされたファイルの
	 * 取得元を表すURLとして格納されていることが期待されます。ただし、難易度表によってはURLが提供されなかったり、
	 * 差分譜面の入手先URLが格納されている場合があります。また、提供されるURLがリンク切れとなっている場合もありますので、
	 * あくまで参考値として活用するようにしてください。</p>
	 * <p>信頼できないサイトが公開している難易度表に記述されたURLでは、悪意のあるサイトへ誘導される可能性があります。
	 * アプリケーションはその点に留意して仕様を決定するようにしてください。</p>
	 * @return 楽曲本体入手先URL。未提供の場合 null。
	 * @since 0.1.0
	 */
	public URL getBodyUrl() {
		return mBodyUrl;
	}

	/**
	 * 差分譜面入手先URLを取得します。
	 * <p>このURLは当該楽曲情報が示す追加(差分)譜面の取得元を表すURLとして格納されていることが期待されます。
	 * 通常、差分譜面は1個のBMSファイルを圧縮したアーカイブファイルとして提供されることが一般的です。
	 * 楽曲本体入手先URLと同様に、URL未提供、リンク切れの可能性があることに留意してください。</p>
	 * <p>信頼できないサイトが公開している難易度表に記述されたURLでは、悪意のあるサイトへ誘導される可能性があります。
	 * アプリケーションはその点に留意して仕様を決定するようにしてください。</p>
	 * @return 差分譜面入手先URL。未提供の場合 null。
	 * @since 0.1.0
	 */
	public URL getAdditionalUrl() {
		return mAdditionalUrl;
	}

	/**
	 * MD5を取得します。
	 * <p>この値は当該楽曲情報のBMSファイル全体のMD5が格納されていることが期待されます。
	 * よって、BMSファイルの内容が1文字でも変化すると異なるハッシュ値を示すようになります。</p>
	 * @return MD5。未提供の場合 null。
	 * @since 0.1.0
	 */
	public String getMd5() {
		return mMd5;
	}

	/**
	 * SHA-256を取得します。
	 * <p>この値は当該楽曲情報のBMSファイル全体のSHA-256が格納されていることが期待されます。
	 * よって、BMSファイルの内容が1文字でも変化すると異なるハッシュ値を示すようになります。</p>
	 * @return SHA-256。未提供の場合 null。
	 * @since 0.1.0
	 */
	public String getSha256() {
		return mSha256;
	}
}
