# BMS LDT - BMS Levelize by Difficulty Tables
このライブラリは一般公開された複数の難易度表を統一フォーマットによりデータベース化し、BMS関連アプリケーション向けにデータベースへのアクセスを提供します。アプリケーションは当ライブラリを使用して各難易度表のダウンロード、登録された楽曲情報の参照・検索を行うことができます。

## 事前準備
ライブラリを使用するにあたり、以下のソフトウェアが必要となります。

### Java Development Kit (JDK)
BMS LDTはJavaで記述されています。ビルドにはJDKが必要になりますので、別途入手・インストールしてください。

### Apache Maven (ビルドツール)
当ライブラリのビルドにはMavenを使用します。Mavenは[こちら](https://maven.apache.org/download.cgi)から入手できます。尚、インストール・設定の手順についてはインターネット等で検索してください。

## ビルド方法
ビルドはコマンドプロンプト等から行います。<br>
このドキュメントが格納されたディレクトリから以下のコマンドを実行し、Mavenのローカルリポジトリにライブラリをインストールしてください。ビルドが成功すると他プロジェクトからライブラリを参照できるようになります。

```
mvn clean install
```

ビルド後のテストを省略したい場合は以下のコマンドを使用してください。

```
mvn clean install -DskipTests
```

## 使用方法
### 他のMavenプロジェクトから使用する
ライブラリを他のMavenプロジェクトから使用したい場合は、当該プロジェクトのpom.xmlの&lt;dependencies&gt;に以下を追加してください。

```
<dependency>
    <groupId>com.lmt</groupId>
    <artifactId>bms-ldt</artifactId>
    <version>0.1.1</version>
</dependency>
```

### ライブラリのリファレンス(Javadoc)
最新版のリファレンスは以下を参照してください。<br>
[https://www.lm-t.com/content/bms-ldt/doc/index.html](https://www.lm-t.com/content/bms-ldt/doc/index.html)

## 変更履歴
[CHANGELOG.md](https://github.com/j-son3/bms-ldt/blob/main/CHANGELOG.md)を参照してください。
