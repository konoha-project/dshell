この章では、 D-Shell のインストール方法について説明します。  

# Linuxシステムへのインストール
***
## 動作環境の確認・準備
本システムを利用するには、以下の動作環境が必要です。

* オペレーティングシステム

* ハードウェア

* 必要なソフトウェア
JRE(Java Runtime Environment) 7  

## パッケージからインストールする場合
***
### Debian系システム
D-Shell サイトよりターゲットとするシステムのパッケージをダウンロードします。  
<pre class="lang:vim decode:true " >
$ wget xxx greentea-0.1-1.x86_64.deb
</pre>

dpkg コマンドでパッケージをインストールします。  

<pre>
$ sudo dpkg -i ./greentea-0.1-1.x86_64.deb
$ dshell
</pre>


### Redhat系システム
D-Shell サイトよりターゲットとするシステムのパッケージをダウンロードします。  
<pre>
$ wget xxx greentea-0.1-1.x86_64.rpm
</pre>

rpm コマンドでパッケージをインストールします。   

<pre>
$ sudo rpm -ivh greentea-0.1-1.x86_64.rpm
$ dshell
</pre>

デフォルトで /usr/local/bin にインストールされます。  

## ソースからインストールする場合
***
D-Shell の Git リポジトリを取得取得します。  
<pre>
$ git clone https://github.com/GreenTeaScript/GreenTeaScript
</pre>
make、ant コマンドを使用し、D-Shell を make します。  
INSTALL_PREFIX にはインストール先のディレクトリを指定してください。  
指定をしなければ、ホームディレクトリへインストールされます。  

<pre>
$ cd GreenTeaScript
$ make
$ make install INSTALL_PREFIX=/usr/local
$ dshell
</pre>
