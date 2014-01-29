この章では、 D-Shell の演算子について説明します。  

# 代数演算子
***
数値の加減乗除に使用します。

例|処理
--|--
- A|負
A + B|加算
A - B|減算
A * B|乗算
A / B|除算
A % B|剰余

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル:  AlgebraicOp.ds" >
function func():boolean {
  var a = 4;
  var b = 2;

  println(-a);
  println(a + b);
  println(a - b);
  println(a * b);
  println(a / b);
  println(a % b);
  return true;
}

func();

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell AlgebraicOp.ds
-4
6
2
8
2
0
</pre>

# 比較演算子
***
同じ数値であることや、違う数値である条件式を表すために使用します。  
各演算子はboolean型の値を返します。  

例|処理
--|--
A == B|等しい
A != B|等しくない
A < B|より少ない
A > B|より多い
A <= B|より少ないか等しい
A >= B|より多いか等しい

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル:  RelationalOp.ds" >
function func():boolean {
  var a = 4;
  var b = 2;
  var c = 2;

  println(a == c);
  println(a != b);
  println(b < a);
  println(a > b);
  println(a <= b);
  println(b >= c);
  return true;
}

func();
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell RelationalOp.ds
true
true
true
true
true
true
</pre>


# 論理演算子
***
複数の条件式を組み合わせた複雑な条件式を表すために使用します。 
各演算子はboolean型の値を返します。  

例|名前|処理
--|--
! A|否定|A が true でない場合 true
A && B|論理積|A および B が共に true の場合に true
A || B|論理和|A または B のどちらかが true の場合に ture

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: LogicalOp.ds" >
function func():boolean {
  var a = 4;
  var b = 2;

  println(!(a == b));
  println((a > b) && (a >= b));
  println((a > b) || (a == b));
  return true;
}
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell LogicalOp.ds
true
true
true
</pre>


# ファイル演算子
***
ファイル・ディレクトリの存在やパーミッションを確認するために使用します。  
各演算子はboolean型の値を返します。  

演算子|条件
---|---
-e A|ファイル、またはディレクトリが存在する
-d A|ディレクトリとして存在する
-f A|ファイルとして存在する
-r A|読み込み可能である
-w A|書き込み可能である
-x A|実行可能である

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル:  FileOp.ds" >
function func():boolean {

  import command chmod, touch, rm, mkdir;

  mkdir /tmp/work

  touch /tmp/work/e
  mkdir /tmp/work/d
  touch /tmp/work/f
  touch /tmp/work/r
  chmod +r /tmp/work/r
  touch /tmp/work/w
  chmod +w /tmp/work/w
  touch /tmp/work/x
  chmod +x /tmp/work/x

  println(-e /tmp/work/e);
  println(-e /tmp/work/d);
  println(-e /tmp/work/f);
  println(-e /tmp/work/r);
  println(-e /tmp/work/w);
  println(-e /tmp/work/x);

  rm -fr /tmp/work

  return true;
}

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell FileOp.ds
true
true
true
true
true
true
</pre>
