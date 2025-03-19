package com.lmt.lib.bldt;

import java.io.IOException;
import java.util.List;

/**
 * 難易度表の元データ解析を行うパーサインターフェイスです。
 *
 * <p>各難易度表の元データはそれぞれデータ形式が異なります。LDTライブラリはデータ形式の差異を吸収し、
 * 統一データフォーマットに変換する仕組みを保有しています。当インターフェイスを実装したパーサはライブラリが
 * 提唱する形式で楽曲情報一覧を返すことで元データの形式差異を吸収します。</p>
 *
 * <p>当ライブラリがサポートするデータ形式のパーサは {@link com.lmt.lib.bldt.parser} パッケージに定義されます。
 * {@link DifficultyTables#add(TableDescription)} で追加する難易度表定義で、これらのパーサを使用できます。</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface Parser {
	/**
	 * 入力された情報を使用して楽曲情報を解析し、楽曲情報一覧を生成して返します。
	 * <p>入力データは各難易度表のサーバからダウンロードされた無加工のバイトデータです。
	 * パーサは与えられた情報を基にして正確なデータの構造を特定し、解析した楽曲情報の一覧を返さなければなりません。
	 * 元データの内容が正しくないと判定した場合は IOException をスローしてエラー終了とするか、
	 * 正しくないデータを無視して解析を続行するかの判断をしてください。処理を続行する場合は
	 * {@link DifficultyTables#printLog(String)} を使用して問題となったデータをログ出力することを推奨します。</p>
	 * @param tableDesc 処理対象の難易度表定義
	 * @param playStyle 処理対象のプレースタイル
	 * @param raw 難易度表のサーバからダウンロードされた無加工のバイトデータ
	 * @return 解析された楽曲情報一覧
	 * @throws IOException 入出力エラー、またはデータ不正の検出により解析が続行不可
	 * @since 0.1.0
	 */
	List<ContentDescription> parse(TableDescription tableDesc, PlayStyle playStyle, byte[] raw) throws IOException;
}
