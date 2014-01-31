この章では、D-Shellの変数と定数の利用について説明します。

# 変数
***
D-Shellで変数を宣言するときには、変数名とデータ型と初期値を指定します。  
変数は次のような構文で宣言します。  

var 変数名: データ型 = 初期値

* 変数宣言と有効範囲
変数は、関数の中でのみ宣言することができます。変数の有効範囲は関数の中となります。  

* データ型指定と型推量
変数宣言時にデータ型を指定しない場合、初期値のデータ型によって変数のデータ型が自動的に決まります。(型推量)  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Variable.ds" >
function func() {
  var age: int = 17
  var name = "uzumaki naruto"

  log age
  log name
}

func()

# 関数外では age, name は参照できない
# log age 未定義の変数の参照(エラー)
# log name 未定義の変数の参照(エラー)
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Variable.ds
17
uzumaki naruto
</pre>

# キャスト
***

* 型キャスト
データ型の変換を明示的に行う場合にキャスト演算子を利用します。
キャスト演算子は、次のような構文で利用します。  

<pre>
(データ型)変数名
</pre>

使用可能なキャストを以下に示します。  
*int float
*float int
*...

* インスタンス判定
オブジェクトがどのデータ型から生成されたかを判断するためには instanceof 演算子を利用します。
instanceof 演算子は、次のような構文で利用します。  

<pre>
変数名 instanceof データ型  
</pre>

変数のデータ型が指定したデータ型と一致した場合、true が返却されます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: TypeCast.ds" >
function func() {
  var a: int = 123
  var b: float = 2.0
  var c: String = "45.67"

  var d = (float)a
  var e = (int)b
  var f = (String)c

  assert(d instanceof (float))
  assert(e instanceof (int))
  assert(f instanceof (String))

  log d
  log e
  log f
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell TypeCast.ds
123.0
2
45.67
</pre>

# 定数
***
D-Shellで定数を宣言するときには、定数名と設定値を指定します。  
変数は次のような構文で宣言します。  

<pre>
let 定数名 = 設定値
</pre>

* 定数宣言と有効範囲
定数は関数内で宣言を行うとローカルなブロックスコープを持ち、関数外で宣言を行うとグローバルなスコープを持つようになります。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Constant.ds" >
let color ="red"

void func() {
  let color = "green"
  log "local color: ${color}"
}

func()

log "top color: ${color}"
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Constant.ds
local color: green
top color: red
</pre>

