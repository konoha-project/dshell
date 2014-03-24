この章では、 D-Shell のインストール方法について説明します。  

# Linuxシステムへのインストール
***
## 動作環境の確認・準備
本システムを利用するには、以下の動作環境が必要です。  

* 必要なソフトウェア  
JRE(Java Runtime Environment) 7  

## パッケージからインストールする場合
***
### Debian系システム
D-Shell サイトよりターゲットとするシステムのパッケージをダウンロードします。  

<pre class="toolbar:0 highlight:0">
$ wget xxx dshell-0.1-1.x86_64.deb
</pre>

dpkg コマンドでパッケージをインストールします。  

<pre class="toolbar:0 highlight:0">
$ sudo dpkg -i ./dshell-0.1-1.x86_64.deb
$ dshell
</pre>


### Redhat系システム
D-Shell サイトよりターゲットとするシステムのパッケージをダウンロードします。  

<pre class="toolbar:0 highlight:0">
$ wget xxx dshell-0.1-1.x86_64.rpm
</pre>

rpm コマンドでパッケージをインストールします。  

<pre class="toolbar:0 highlight:0">
$ sudo rpm -ivh dshell-0.1-1.x86_64.rpm
$ dshell
</pre>

デフォルトで /usr/local/bin にインストールされます。  

## ソースからインストールする場合
***
D-Shell の Git リポジトリを取得取得します。  

<pre class="toolbar:0 highlight:0">
$ git clone https://github.com/konoha-project/dshell
</pre>

make、ant コマンドを使用し、D-Shell を make します。  
INSTALL_PREFIX にはインストール先のディレクトリを指定してください。  
指定をしなければ、ホームディレクトリへインストールされます。  

<pre class="toolbar:0 highlight:0">
$ cd dshell
$ make
$ make install INSTALL_PREFIX=/usr/local
$ dshell
</pre>
