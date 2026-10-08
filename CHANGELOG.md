# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [0.4.1] - 2026-10-08
### Changed
- 難易度表ツールの親画面が常に手前に表示される設定の場合、ツールの画面も常に手前に表示するように変更しました。

## [0.4.0] - 2026-08-12
### Added
- Stardust難易度表をサポートしました。(Presets.STARDUST)
- Luminous難易度表をサポートしました。(Presets.LUMINOUS)
- 皿難易度表(3rd)をサポートしました。(Presets.SCRATCH_3RD)
- 難易度表の状態を取得する TableDescription#getStatus を追加しました。
- BMS Analysis v1.0.0 に搭載された難易度表ツールを当ライブラリに移植しました。それに伴い、以下の機能追加を行いました。
    - 難易度表ツールを表示する DifficultyTables#guiBrowse を追加しました。
    - 難易度表更新画面を表示する DifficultyTables#guiUpdate を追加しました。
    - DifficultyTables#main の動作モードに browse を追加しました。

### Changed
- ContentDatabase#update(HttpClient, String, Duration, UpdateProgress) の戻り値を void から UpdateResult に変更しました。

### Fixed
- 難易度表更新時、楽曲情報の元データ取得リクエスト送信でエラー・タイムアウトが発生した時にプログレス通知されない不具合を修正しました。

## [0.3.0] - 2026-07-13
### Added
- Starlight難易度表をサポートしました。(Presets.STARLIGHT)

### Changed
- GENOCIDE通常/発狂難易度表のデータ取得元を変更しました。

## [0.2.0] - 2025-08-04
### Added
- Solar難易度表をサポートしました。(Presets.SOLAR)
- Supernova難易度表をサポートしました。(Presets.SUPERNOVA)
- ContentDatabase#update() で、難易度表更新途中で例外がスローされても全難易度表の更新が実行されるオーバーロードメソッドを追加しました。

## [0.1.1] - 2025-06-20
### Added
- 癖譜面ライブラリーを標準サポートしました。(Presets.UNIQUE)

## [0.1.0] - 2025-03-19
### Added
- 新規作成。
