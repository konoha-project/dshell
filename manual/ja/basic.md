この章では、 D-Shell の基本的な構文について説明します。  

# エントリーポイント
***
D-Shell では、C や Java と同様にトップレベルにメイン関数を定義することができます。  
メイン関数は、スクリプトデータのすべての評価がおわったあと、自動的に呼び出される関数です。  
メイン関数を定義するには、ユーザ定義関数と同様に function ステートメントを使用します。  

<pre>
@Export function main() {
  関数の処理
}
</pre>

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: EntryPoint.ds" >
@Export function main() {
  var name = "naruto";
  var age = 17;
  println("${name} : ${age}")
}
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell EntryPoint.ds p1 p2
naruto : 17
</pre>

# 命令の分離(処理の区切り)
***
D-Shellでは、C や Java と同様に命令の区切りにはセミコロンが必要となります。  

<pre>
var num = 1;
println("Hello");
</pre>

# コマンドラインオプションの利用
***
D-Shellでは、スクリプトを実行するときに、コマンドラインからプログラムにコマンドラインオプションの値を渡すことができます。  
コマンドラインオプションは、プログラムでは ARGV: Array&lt;String&gt; というグローバル変数に代入されます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Commandline.ds" >
function func() {
  var i = 0;
  for(i=0; i&lt;ARGV.length; i++) {
    println(ARGV[i]);
  }
}

func();

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
プログラムから画面への出力は print 関数または println 関数を使用します。  

* println関数
println 関数は、自動的に末尾に改行が付加されます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Println.ds" >
println("Hello, World.");
println(123);
println(true);
print
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Println.ds
Hello, World.
123
true
</pre>

* print 関数
print 関数は、引数がそのまま標準出力へ表示されます。  
改行の自動的な付加は行われません。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Print.ds" >
print("Hello, World.");
print(123);
print(true);
print
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Print.ds
Hello, World.123true
</pre>

# 予約語
D-SHell では、以下の予約語があり、予約語と同一の変数名や関数名を使うことはできません。 

* 予約語一覧
