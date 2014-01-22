この章では、 D-Shell の基本的な構文について説明します。  

# メイン関数
***
D-Shell では、Java や C と同様にトップレベルにメイン関数を定義することができます。  
メイン関数は、スクリプトデータのすべての評価がおわったあと、自動的に呼び出される関数です。  

<pre>
@Export function main() {

  var name = "naruto";
  var age = 17;
  println("${name} : ${age}")

}
</pre>

# 命令の分離(処理の区切り)
***
C や Perl と同様に、D-Shell でも命令の区切りにはセミコロンが必要となります。  

<pre class="toolbar:1" title="サンプル">
var num = 1;
println("Hello!");
</pre>

# コマンドラインオプションの利用
***
D-Shellでは、スクリプトを実行するときにコマンドラインからプログラムにコマンドラインオプションの値を渡すことができます。
コマンドラインオプションは、プログラムでは ARGV: Array&lt;String&gt; というグローバル変数に代入されます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Argv.ds" >
function func() {
  var i = 0;
  for(i=0; i&lt;ARGV.length; i++) {
    println(ARGV[i]);
  }
}

func();

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Argv.ds p1 p2
argv.ds
p1
p2
</pre>

# コメント
***
コメントは、プログラムから無視される意味解釈のされない字句です。  
単一行コメントと複数行コメントの2 種類あります。  

* 単一行コメント  
<pre class="toolbar:1" title="コメント例">
# BashやPythonライクな一行コメントです
// C++やJavaライクな一行コメントです
</pre>

* 複数行コメント  

<pre class="toolbar:1" title="コメント例">
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
プログラムから画面への出力は print 関数か println 関数を使用します。  

* println関数
println 関数は、自動的に末尾に改行が付加されます。  

<pre class="toolbar:1" title="サンプル">
println("Hello, World!");
</pre>

* print 関数
print 関数は、引数がそのまま標準出力へ表示されます。  
改行の自動的な付加は行われません。  

<pre class="toolbar:1" title="サンプル">
print("Hello, World!!\n");
</pre>

