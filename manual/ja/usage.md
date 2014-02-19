この章では、 D-Shell の起動方法について説明します。 

# D-Shell の起動
***
D-Shell はコンソールから次のようなコマンドで起動することができます。  

<pre class="toolbar:1 highlight:0" title="使用方法">
dshell [オプション]... [シェルスクリプト [引数 ...]]
</pre>

* オプション  
--logging:file [ファイル名]  
内部コマンド log の出力先を指定したファイルに指定します。  
ファイルが存在していた場合、追記します。  
--logging:stdout  
内部コマンド log の出力先を標準出力に指定します。  
--logging:stderr  
内部コマンド log の出力先を標準エラー出力に指定します。  
--logging:syslog [ホストのIPアドレス]  
内部コマンド log の出力先を指定したホストの syslog に指定します。  
ホストのIPアドレスを省略した場合、localホストの syslog に出力します。  
--debug  
デバッグモードで dshell を実行します。  
--help  
このコマンドの使用方法を標準出力に出力し、コマンドを終了します。  
--rec [RECのURL]  
シェルスクリプトの実行結果をRECに送信します。  
バッチ・モードのみ有効なオプションです。  
--version  
このコマンドのバージョン情報を標準出力に出力し、コマンドを終了します。  

* シェルスクリプト  
実行するシェルスクリプトのファイル名を引数と共に指定します。  
シェルスクリプトを指定した場合、D-Shell をバッチモードで起動します。  
省略した場合、D-Shell を対話モードで起動します。  


# 対話モードとバッチモード
***
D-Shell は一般的な OS シェルと同様に、対話モードとバッチモードで使用することができます。  

* 対話モード
対話モードで使用する場合、ユーザはコンソールから直接コマンドを入力します。  


<pre class="toolbar:0 highlight:0">
$ dshell
D-Shell, version 0.1 (Java JVM-1.7.0_45)
Copyright (c) 2013-2014, Konoha project authors
hogehoge:~> log "Hello"
Hello
</pre>

* バッチモード
バッチ・モードで使用する場合、dshell コマンドにコマンドオプションを含むファイルの名前を指定して、事前に準備したシェルスクリプトを実行します。  

<pre class="toolbar:0 highlight:0">
$ cat test.ds
log "Hello"
$ dshell ./test.ds
Hello
</pre>

