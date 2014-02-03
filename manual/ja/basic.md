この章では、 D-Shell の基本的な構文について説明します。  

# エントリポイント
***
D-Shell には、C や Java のような main 関数がありません。  
対話モードでは、プロンプトに入力された順に命令を実行します。  
バッチモードでは、指定したスクリプトファイルの先頭から順番に命令を実行します。  

# 命令の分離(処理の区切り)
***
D-Shellでは、命令の区切りは改行です。  
複数のコマンドをセミコロン(;)で区切り、連続して実行することもできます。  
(セミコロンで改行が入力されたのと同じ扱いとなります)  

# コマンドラインオプションの利用
***
D-Shellでは、スクリプトを実行するときに、コマンドラインからプログラムにコマンドラインオプションの値を渡すことができます。  
コマンドラインオプションは、プログラムでは ARGV: Array&lt;String&gt; というグローバル変数に代入されます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Commandline.ds" >
function func() {
  for(arg in ARGV) {
    log arg
  }
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Commandline.ds p1 p2
argv.ds
p1
p2
</pre>

# コメント
***
コメントは、プログラムから無視される意味解釈のされない字句です。  
単一行コメントと複数行コメントの２種類があります。  

* 単一行コメント  
<pre>
# BashやPythonライクな一行コメントです
// C++やJavaライクな一行コメントです
</pre>

* 複数行コメント  
<pre>
/*
  C言語ライクな複数行コメントです
*/
/*
  /*
  複数行コメントのネストも可能です
  */
*/
</pre>

# 画面への出力
***

* 内部コマンドの利用( log コマンド)
D-Shell にビルトインコマンドとして組み込まれている log コマンドを利用する例です。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: OutputLog.ds" >
function func() {
  log "Hello, World."
  log 123
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell OutputLog.ds
Hello, World.
123
</pre>

* 外部コマンドの利用( echo コマンド)
外部コマンドの echo を D-Shell にインポートして利用する例です。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: OutputEcho.ds" >
import command echo
function func() {
  echo "Hello, World."
  echo 123
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell OutputEcho.ds
Hello, World.
123
</pre>

* Javaメソッドの利用( println 関数)
Java の println 関数を D-Shell にインポートして利用する例です。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: OutputPrintln.ds" >
import java.lang.System
function func() {
  System.out.println("Hello, World.")
  System.out.println(123)
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell OutputPrintln.ds
Hello, World.
123
</pre>

# 予約語
D-Shell では、制御構造は糖衣構文により再定義することが可能なため、予約語の制限はありません。 

