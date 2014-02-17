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
function func() {
  var a = 4
  var b = 2

  log ${-a}
  log ${a + b}
  log ${a - b}
  log ${a * b}
  log ${a / b}
  log ${a % b}

  return
}

func()
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
function func() {
  var a = 4
  var b = 2
  var c = 2

  log ${a == c}
  log ${a != b}
  log ${b < a}
  log ${a > b}
  log ${a <= b}
  log ${b >= c}
  return
}

func()
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
  var a = 4
  var b = 2

  log ${!(a == b)}
  log ${(a > b) && (a >= b)}
  log ${(a > b) || (a == b)}
  return true
}
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell LogicalOp.ds
true
true
true
</pre>

# パターンマッチング演算子
***
正規表現の条件式を表すために使用します。  
例|名前|処理
--|--
A =~ B|正規表現|文字列 A が 正規表現パターン B を満たす場合に true

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Regex.ds" >
function func() {
  var str = "abc"
  if (str =~ "^a") {
    log "match!"
  }
  else {
    log "not match!"
  }
  if (str =~ "^b") {
    log "match!"
  }
  else {
    log "not match!"
  }
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Regex.ds
match!
not match!
</pre>

