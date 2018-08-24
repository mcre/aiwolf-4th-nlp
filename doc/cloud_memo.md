実行の例なので、不要な部分は適宜読み飛ばしてください。

## 1. Azure登録

* https://azure.microsoft.com/ja-jp/free/

## 2. VM作成

1. https://portal.azure.com/
2. リソースの作成 -> compute -> Ubuntu Server 18.04 LTS -> Resource Manager
3. いろいろ入力
    - 名前: aiwolf01
    - SSD
    - ユーザ名: m_cre
    - 認証: SSH
    - SSH公開キー: 入力
    - サブスクリプション: 無料試用版
    - リソースグループ: aiwolf
    - 場所: 西日本(自然言語サーバが西日本(推測)っぽいので)
    - サイズ: B1s(使いながら上げる想定)
    - オプションはパブリック受信ポートのSSHをONにする以外は一旦デフォルト

## 3. Dockerインストール

vm

```
$ sudo apt-get update
$ sudo apt-get install apt-transport-https ca-certificates curl software-properties-common
$ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
$ sudo apt-key fingerprint 0EBFCD88
$ sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
$ sudo apt-get update
$ sudo apt-get install docker-ce
```

## 4. 必要なjarライブラリを準備

* local(事前に必要な.jarを準備)
    - 必要な.jarのリストは`README.md`を参照してください。

```
$ zip -j ~/Desktop/lib.zip ~/GoogleDrive/Documents/programming/java/aiwolf/aiwolf-4th-nlp/lib/*.jar
$ scp ~/Desktop/lib.zip m_cre@XXX.XXX.XXX.XXX:~/
```

## 5. Docker Container 作成 & 必要な初期設定

vm

```
$ sudo docker pull mcre/java10-juman-knp
$ sudo docker run -itd --name aiwolf mcre/java10-juman-knp
$ sudo docker cp ~/lib.zip aiwolf:/root/lib.zip
$ sudo docker exec -it aiwolf bash
$ git clone https://github.com/mcre/aiwolf-4th-nlp.git
$ unzip -d ~/aiwolf-4th-nlp/lib/ ~/lib.zip
$ # 辞書の設定
$ nano ~/usr/etc/jumanrc
$ ## 「辞書ファイル」に`/root/aiwolf-4th-nlp/dic`を追加
$ # jumanとknpの環境変数の設定
$ echo 'export PathJuman=/root/usr/bin/juman' >>~/.bashrc
$ echo 'export PathKNP=/root/usr/bin/knp' >>~/.bashrc
$ source ~/.bashrc
```

## 6. 実行

* コンテナの中に入る

```
$ sudo docker start aiwolf
$ sudo docker exec -it aiwolf bash
```

* ソースコード変更時

```
$ cd ~/aiwolf-4th-nlp/
$ git pull
$ util/compile.sh
```

* 辞書変更時

```
$ cd ~/aiwolf-4th-nlp/dic
$ ./makedic.sh
```

* 実行例

```
$ cd ~/aiwolf-4th-nlp/
$ util/main.sh
$ util/main.sh type=remote5 server=****.net port=10000
$ nohup util/main.sh type=remote5 server=****.net port=10000 > log.txt &
```



----------------------------------------------------------

以降作業メモ

## Azure登録

* https://azure.microsoft.com/ja-jp/free/

## VM作成

1. https://portal.azure.com/
2. リソースの作成 -> compute -> Ubuntu Server 18.04 LTS -> Resource Manager
3. いろいろ入力
    - 名前: aiwolf01
    - SSD
    - ユーザ名: m_cre
    - 認証: SSH
    - SSH公開キー: 入力
    - サブスクリプション: 無料試用版
    - リソースグループ: aiwolf
    - 場所: 西日本(自然言語サーバが西日本(推測)っぽいので)
    - サイズ: B1s(使いながら上げる想定)
    - オプションはパブリック受信ポートのSSHをONにする以外は一旦デフォルト

## 接続テスト

* `ssh m_cre@XXX.XXX.XXX.XXX`
    - OK

## Dockerインストール

https://docs.docker.com/install/linux/docker-ce/ubuntu/

```
$ sudo apt-get update
$ sudo apt-get install apt-transport-https ca-certificates curl software-properties-common
$ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
$ sudo apt-key fingerprint 0EBFCD88
$ sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
$ sudo apt-get update
$ sudo apt-get install docker-ce
```

## Docker Container作成実験

```
$ sudo docker run -it --rm openjdk:10-jre-slim bash
$ apt-get update
$ apt-get install juman
$ # この方法だとknpのインストールに不都合があるそうなのでやめ
$ exit
```

```
$ sudo docker run -it --name test openjdk:10-jre-slim bash
$ apt-get update
$ apt-get install -y wget build-essential libcdb-dev libjuman zlib1g-dev
$ cd ~
$ wget -O juman-7.01.tar.bz2 http://nlp.ist.i.kyoto-u.ac.jp/nl-resource/juman/juman-7.01.tar.bz2
$ wget -O knp-4.19.tar.bz2 http://nlp.ist.i.kyoto-u.ac.jp/nl-resource/knp/knp-4.19.tar.bz2
$ tar jxvf juman-7.01.tar.bz2
$ tar jxvf knp-4.19.tar.bz2
$ mkdir usr
$ cd ~/juman-7.01
$ ./configure --prefix=/root/usr/ # デフォルトだと.jumanrc まわりのエラーがでるようなので適当なところに変更
$ make
$ make install
$ cd ~/knp-4.19
$ ./configure --prefix=/root/usr/ -with-juman-prefix=/root/usr/
$ make
$ make install
$ PATH=$HOME/usr/bin:$PATH
$ echo "麻生太郎はコーヒーを買って飲んだ。" | juman | knp -simple -anaphora
$ exit
```

## DockerFile作成テスト

```
$ mkdir ~/java10-juman-knp
$ cd ~/java10-juman-knp
$ vi Dockerfile
```

https://github.com/mcre/docker-java10-juman-knp/blob/master/Dockerfile 参照

```
$ sudo docker build -t mcre/java10-juman-knp .
$ sudo docker run -it --rm mcre/java10-juman-knp
$ echo "麻生太郎はコーヒーを買って飲んだ。" | juman | knp -simple -anaphora
$ exit
```

## DockerImageの公開

* 上記をもとにgithubリポジトリを作成。
    - https://github.com/mcre/docker-java10-juman-knp
* Setting -> Integrations & Services -> add service -> Docker # この設定いらないかも？
* DockerhubからCreate Automated Build -> Github
    - name: java10-juman-knp
* DockerhubのBuildSettingからTriggerボタンを押してしばらく待つとイメージができあがる

## 一旦いろいろ掃除

```
$ cd ~
$ rm -rf java10-juman-knp/
$ sudo docker rm -f test
$ sudo docker image prune
$ sudo docker rmi hogehoge hogehoge
```

## つくったDockerImage上でプログラムを動かせるようにする実験

vm

```
$ sudo docker pull mcre/java10-juman-knp
$ sudo docker run -it --name aiwolf -p 10000:10000 mcre/java10-juman-knp
$ git clone https://github.com/mcre/aiwolf-4th-nlp.git
$ exit
```

local

```
$ zip -j ~/Desktop/lib.zip ~/GoogleDrive/Documents/programming/java/aiwolf/aiwolf-4th-nlp/lib/*.jar
$ scp ~/Desktop/lib.zip m_cre@XXX.XXX.XXX.XXX:~/
```

vm

```
$ sudo docker cp ~/lib.zip aiwolf:/root/lib.zip
$ sudo docker exec -it aiwolf bash
$ unzip -d ~/aiwolf-4th-nlp/lib/ ~/lib.zip
$ cd ~/aiwolf-4th-nlp/
$ javac -d bin -cp lib/aiwolf-client.jar:lib/aiwolf-common.jar:lib/aiwolf-server.jar:lib/aiwolf-viewer.jar:lib/jackson-core-2.8.1.jar:lib/jackson-databind-2.8.5.jar:lib/java-juman-knp.jar:lib/jsonic-1.3.10.jar:lib/McrePlayer.jar:lib/jackson-annotations-2.8.0.jar src/net/mchs_u/mc/aiwolf/nlp/starter/*.java src/net/mchs_u/mc/aiwolf/nlp/util/*.java src/net/mchs_u/mc/aiwolf/nlp/human/*.java src/net/mchs_u/mc/aiwolf/nlp/common/*.java src/net/mchs_u/mc/aiwolf/nlp/chaser/*.java
$ java -cp bin:lib/aiwolf-client.jar:lib/aiwolf-common.jar:lib/aiwolf-server.jar:lib/aiwolf-viewer.jar:lib/jackson-core-2.8.1.jar:lib/jackson-databind-2.8.5.jar:lib/java-juman-knp.jar:lib/jsonic-1.3.10.jar:lib/McrePlayer.jar:lib/jackson-annotations-2.8.0.jar net.mchs_u.mc.aiwolf.nlp.starter.Main
$ exit
$ sudo docker rm -f aiwolf
```

長いので.shにまとめる。リポジトリ参照。

```
$ sudo docker pull mcre/java10-juman-knp
$ sudo docker run -itd --name aiwolf -p 10000:10000 mcre/java10-juman-knp
$ sudo docker cp ~/lib.zip aiwolf:/root/lib.zip
$ sudo docker exec -it aiwolf bash
$ git clone https://github.com/mcre/aiwolf-4th-nlp.git
$ unzip -d ~/aiwolf-4th-nlp/lib/ ~/lib.zip
$ cd ~/aiwolf-4th-nlp/
$ util/compile.sh
$ util/main.sh # OK
$ util/main.sh type=remote5 server=****.net port=10000 # OK AzureVM側の設定は特に変えなくてもいけた
$ exit
$ sudo docker rm -f aiwolf
```

docker run 時の-pオプションがなくてもいけるかどうか調べる

```
$ sudo docker run -itd --name aiwolf mcre/java10-juman-knp
$ sudo docker cp ~/lib.zip aiwolf:/root/lib.zip
$ sudo docker exec -it aiwolf bash
$ git clone https://github.com/mcre/aiwolf-4th-nlp.git
$ unzip -d ~/aiwolf-4th-nlp/lib/ ~/lib.zip
$ cd ~/aiwolf-4th-nlp/
$ util/compile.sh
$ util/main.sh # OK
$ util/main.sh type=remote5 server=****.net port=10000 # いけた
```

辞書のコンパイル

```
$ cd ~/aiwolf-4th-nlp/dic
$ ./makedic.sh
```

追加辞書の設定

```
$ apt-get install nano
$ nano ~/usr/etc/jumanrc
```

辞書ファイルに追加

```
/root/aiwolf-4th-nlp/dic
```

チェック

```
$ echo "人狼知能は楽しい"  | juman
```
