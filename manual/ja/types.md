この章では、 D-Shell でサポートするデータ型について説明します。 

D-Shellでは次のようなデータ型が準備されています。  
扱う値の範囲や種類によって適切なデータ型を選択してください。  

# boolean 型
***
論理型は、真偽の値を扱う最も単純なデータ型です。  
リテラルは「真 = true」と「偽 = false」の値のどちらかになります。  
C/C++ と異なり、1 や 0 のような整数値は、論理値の代わりには取り扱うことができません。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Boolean.ds" >
function func() {

  var a: boolean = true;
  var b = false;

  println(a);
  println(b);

  assert(a instanceof boolean);
  assert(b instanceof boolean);
}

func();
true
false
</pre>


<pre class="toolbar:1" title="実行例">
$ dshell Boolean.ds
</pre>


# int 型
***
int は整数のデータ型です。リテラルは整数のみ指定可能です。  
整数の精度としては 64 ビット符号付きです。  
整数と浮動小数点数を混在させて演算した場合、整数は自動的に浮動小数点数に変換されます。  
浮動小数点数から整数に変換するときはキャスト演算子を使います。  
(キャストは、単純に小数点以下が切り捨てになります。)  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Int.ds" >
function func(): boolean {

  var a = 10;
  var b = -1;
  var c:int = 9223372036854775807;

  println(a);
  assert(a instanceof int);
  println(b);
  assert(b instanceof int);
  println(c);
  assert(c instanceof int);
  return true;
}
func();
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Int.ds
10
-1
9223372036854775807
</pre>

# float 型
***
float は浮動小数点数 を扱うデータ型です。精度としては 64 ビット(倍精度)です。  
浮動小数点のリテラルは実数、または常用対数eによる表現の2通りの方法で記述することができます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Float.ds" >
function func(): boolean {

  var a = 3.14;
  var b: float = 0.5e3;

  println(a);
  assert(a instanceof float);
  println(b);
  assert(b instanceof float);
  return true;
}

func();

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Float.ds
3.14
true
500.0
true
</pre>

# String 型
***
String は、連結された文字列を扱うデータ型です。  
1文字1文字が連なって格納されてるため、文字の検索や置換、文字列の切り出しといった操作が簡単にできます。  
文字列長は、文字コードのバイト数に関わらず、文字数でカウントします。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: String.ds" >
function func() {
  var str: String = "うずまきナルト";
  println(str);
  assert(str instanceof String);
}

func();
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell String.ds
うずまきナルト
true
</pre>

String 型に格納できる文字数は使えるメモリ (heap memory) のサイズに依存します。  

## 文字列のリテラルとエスケープ
文字列リテラルは、シングルクォート・ダブルクォートの２通りの方法で記述することができます。  
ダブルクォート(")で囲まれた文字列では、エスケープシーケンスを利用し、出力することができます。  
エスケープシーケンスは次の5種類が定義されています。  

記号|意味
---|---
\n|改行 (OS 非依存)
\t|タブ
\'|シングルクオート
\"|ダブルクオート
\\|\記号


シングルクォート(')で囲まれた文字列では、エスケープシーケンスを書いても特別な効果は得られず、書いたままの文字列で出力されます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: EscSeq.ds" >
function func() {
  println("うずまき\nナルト");
  println('うずまき\nナルト');
  println(str);
}

func();
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell EscSeq.ds
うずまき
ナルト
うずまき\nナルト
</pre>

ダブルクオーテーションで囲まれた文字列(それに相当する文字列)の中には${式}という形式で式 の内容(を文字列化したもの)を埋め込むことができます。  
明示的に式展開を止めるには$の前にバックスラッシュを置きます。 

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Interpolation.ds" >
println("西暦${1900 + 114}年");
println("西暦\${1900 + 114}年");
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Interpolation.ds
西暦2014年
西暦${1900 + 114}年
</pre>


## 文字列操作
String 型は、Java のパッケージである java.lang.String のメソッドを同じように呼び出し、文字列を操作することができます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ReplaceAll.ds" >
function func() {
  var str: String = "はるのサクラ";
  str.replaceAll("はるの", "うずまき");
  println(str);
}

func();
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ReplaceAll.ds
うずまきサクラ
</pre>

# void 型
***
返り値の型が void である場合は、 返り値に意味がないことを表します。  
関数定義のパラメータで void が使用されている場合は、 その関数がパラメータを受け付けないことを表します。  
戻り値に void が使用されている場合は、その関数が戻り値を返却しないことを表します。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Void.ds" >
function func(void): void {
  println("function call!");
  return;
}

func();
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Void.ds
function call!
</pre>

# Array 型(T[]型)
***
配列型は、複数個のオブジェクトをコレクションとして扱う最も基本的なデータ構造です。  
配列クラスを使用すると、配列にアクセスして操作することができます。  
配列インデックスは 0 から始まります。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Array.ds" >
function func() {
  var a: Array<int> = [12, 34, 56, 78, 90];
  var b = ["hoge", "piyo", "fuga"];

  assert(a instanceof Array<int>);
  assert(b instanceof Array<String>);

  var i: int = 0;

  while(i < a.length) {
    println(a[i]);
  }

  i = 0;

  while(i < b.length) {
    println(b[i]);
  }
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Array.ds
12
34
56
78
90
hoge
piyo
fuga
</pre>

# Map<T> 型
***
Map 型は、キーと値のペアを持ち、キーを使いそれとペアとなっている値を扱うことができるデータ構造です。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: HashMap.ds" >
function func() {

  var map: Map<int> = {"hoge": 3, "fuga": 5};
  println(map["hoge"]);
  assert(map instanceof Map<int>);
}

func();
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell HashMap.ds
int
</pre>

# Func<TResult, T1, T2..> 型
***
Func 型は、メソッドを参照するための型、いわゆるデリゲートです。
宣言<>内の第二引数(T2...)以降の値を受け取り、第一引数(TResult)に指定された型の値を返す関数をオブジェクト化します。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Func.ds" >
var func: Func<int, int> = function(x: int): int {
  return x * 2;
}

println(func(3));
println(func instanceof Func<int, int>);
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Func.ds
6
true
</pre>

