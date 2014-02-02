この章では、 D-Shell の起動方法について説明します。 

# D-Shell の起動
***
D-Shell はコンソールから次のようなコマンドで起動することができます。

使用方法: dshell [オプション]... [シェルスクリプト [引数 ...]]

* オプション
--log ログファイル名  
内部コマンド log で出力するログの出力先を指定します。  
省略した場合、標準出力に出力します。  

--debug  
デバッグモードで dshell を実行します。  

--help  
このコマンドの使用方法を標準出力に出力し、コマンドを終了します。  

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

<pre>
$ dshell
D-Shell, version 0.1 (Java JVM-1.7.0_45)
Copyright (c) 2013-2014, Konoha project authors
hogehoge:~> log "Hello"
Hello
</pre>

* バッチモード
バッチ・モードで使用する場合、dshell コマンドにコマンドオプションを含むファイルの名前を指定して、事前に準備したシェルスクリプトを実行します。  

<pre>
$ cat test.ds
log "Hello"
$ dshell ./test.ds
Hello
</pre>
