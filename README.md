チームm_cre (自然言語処理部門)
====

[第4回人狼知能大会 自然言語処理部門](http://aiwolf.org/4th-aiwolf-contest)の出場エージェントです。

## プログラム解説

* [第3回出場エージェント](https://github.com/mcre/aiwolf-3rd-nlp) をベースにしています。
* 更新したら記載予定

## 動作環境

* macOS (High Sierra)
* jdk (8)

### OSにインストールするもの

* juman (7.01)
  + ```dic/makedic.sh```を参考に辞書をコンパイルしてください
  + [このへん](http://d.hatena.ne.jp/knaka20blue/20110320/1300627864)を参考に```~/.jumanrc```に```dic```(フルパスで)を設定してください。
  + インストール先が```/usr/local/bin/juman```ではない場合はClauseクラスから呼んでいるKNPのコンストラクタの記載を変更する必要があります。
  + (スペックによると思いますが)juman++だと動作が遅くて応答制限時間を満たさない場合があるので、jumanのほうが良いと思います。

* knp (4.16)
  + インストール先が```/usr/local/bin/knp```ではない場合はClauseクラスから呼んでいるKNPのコンストラクタの記載を変更する必要があります。

### 必要ライブラリ等

* aiwolf-client.jar (0.4.11)
* aiwolf-common.jar (0.4.11)
* aiwolf-server.jar (0.4.11)
* aiwolf-viewer.jar (0.4.11)
* jsonic-1.3.10.jar
* jackson-core-2.8.1.jar
* jackson-annotations-2.8.0.jar
* jackson-databind-2.8.5.jar
* java-juman-knp.jar
  + [mychaelstyle/java-juman-knp](https://github.com/mychaelstyle/java-juman-knp) を(勝手に)jarに固めたもの
* [McrePlayer.jar](https://github.com/mcre/aiwolf-3rd/blob/master/McrePlayer.jar)

## クラス等説明

* プレイヤー本体
  * net.mchs_u.mc.aiwolf.nlp.chaser.McreNlpPlayer

* mainメソッドが含まれるクラス
  + net.mchs_u.mc.aiwolf.nlp.starter.Main
    - サーバとクライアント5体が一発で起動します
  + net.mchs_u.mc.aiwolf.nlp.chaser.Clause
    - 文章を解析してClauseクラスに格納された結果を確認できます
  + net.mchs_u.mc.aiwolf.nlp.util.KNPChecker
    - Clauseクラスに格納されるまえの詳細な解析結果を確認できます
  + net.mchs_u.mc.aiwolf.nlp.util.LogConverter
    - サーバから吐き出されるlogファイルを読みやすく変換できます
  + net.mchs_u.mc.aiwolf.nlp.util.TransratedMapChecker
    - 自然言語をプロトコルに変換した履歴を確認できます
    
* 他
  + dic/translatedMap.dat
    - 自然言語からプロトコルに変換した場合、その結果を保存し、次回同一の自然言語の入力があった場合に形態素解析を省略することで応答の速度を上げます。
    - Earのロジックを変更した場合などは一度手動で削除しないと意図した動作にならないと思います。

## 連絡先

* [twitter: @m_cre](https://twitter.com/m_cre)
* [blog](http://www.mchs-u.net/mc/)

## License

* MIT
  + see LICENSE
