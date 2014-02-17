この章では、 D-Shell でサポートするデータ型について説明します。 

D-Shellでは、次のようなデータ型が準備されています。  
扱う値の範囲や種類によって適切なデータ型を選択してください。  

# boolean 型
***
論理型は、真偽の値を扱う最も単純なデータ型です。  
リテラルは「真 = true」と「偽 = false」の値のどちらかになります。  
C/C++ と異なり、1 や 0 のような整数値は、論理値の代わりには取り扱うことができません。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Boolean.ds" >
function func() {

  var a: boolean = true
  var b = false

  log ${a}
  log ${b}

  assert(a instanceof boolean)
  assert(b instanceof boolean)

  return
}

func()
</pre>


<pre class="toolbar:1" title="実行例">
$ dshell Boolean.ds
true
false
</pre>


# int 型
***
int は整数のデータ型です。
リテラルは整数のみ指定可能です。精度は 64 ビット符号付きです。  
整数と浮動小数点数を混在させて演算した場合、整数は自動的に浮動小数点数に変換されます。  
浮動小数点数から整数に変換するときはキャスト演算子を使います。  
(キャストは、単純に小数点以下が切り捨てになります。)  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Int.ds" >
function func() {

  var a = 10
  var b = -1
  var c:int = 9223372036854775807

  log ${a}
  assert(a instanceof int)
  log ${b}
  assert(b instanceof int)
  log ${c}
  assert(c instanceof int)

  return
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Int.ds
10
-1
9223372036854775807
</pre>

# float 型
***
float は浮動小数点数 を扱うデータ型です。精度は 64 ビット(倍精度)です。  
浮動小数点のリテラルは実数、または常用対数eによる表現の2通りの方法で記述することができます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Float.ds" >
function func() {

  var a = 3.14
  var b: float = 0.5e3

  log ${a}
  assert(a instanceof float)
  log ${b}
  assert(b instanceof float)

  return
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Float.ds
3.14
500.0
</pre>

# String 型
***
String は、連結された文字列を扱うデータ型です。  
1文字1文字が連なって格納されてるため、文字の検索や置換、文字列の切り出しといった操作が簡単にできます。  
文字列長は、文字コードのバイト数に関わらず、文字数でカウントします。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: String.ds" >
function func() {

  var str = "うずまきナルト"
  log ${str}
  assert(str instanceof String)

  return
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell String.ds
うずまきナルト
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

  log "うずまき\nナルト"
  log 'うずまき\nナルト'

  return
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell EscSeq.ds
うずまき
ナルト
うずまき\nナルト
</pre>

## 式展開
ダブルクオーテーションで囲まれた文字列(それに相当する文字列)の中には${式}という形式で式の内容(を文字列化したもの)を埋め込むことができます。  
明示的に式展開を止めるには$の前にバックスラッシュを置きます。 

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Interpolation.ds" >
log "西暦${1900 + 114}年"
log "西暦\${1900 + 114}年"
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

  var str:String = "はるのサクラ"
  str.replaceAll("はるの", "うずまき")
  log ${str}

  return
}

func()
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
function func():void {

  log "function call"
  return
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Void.ds
function call
</pre>

# Array 型(T[]型)
***
Array 型は、複数個のオブジェクトをコレクションとして扱う最も基本的なデータ構造です。  
配列クラスを使用すると、配列にアクセスして操作することができます。  
配列インデックスは 0 から始まります。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Array.ds" >
function func() {

  var a: int[] = [12, 34, 56, 78, 90]
  var b = ["hoge", "piyo", "fuga"]

  assert(a instanceof int[])
  assert(b instanceof String[])

  for(x in a) {
    log ${x}
  }

  for(y in b) {
    log ${y}
  }

  return
}

func()
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
  var map: Map<int> = { "hoge": 3, "piyo": 5, "fuga": 7 }
  for(key in map) {
    log "${key} => ${map[key]}"
  }
  assert(map instanceof Map<int>)

  return
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell HashMap.ds
3
5
7
</pre>

# Func<T, U, V, ...> 型
***
Func 型は、定義済み関数を関数オブジェクトとして格納するデータ型です。
データ型 T を戻り値のデータ型, U以降を引数のデータ型とする関数をオブジェクト化します。

<pre>
Func<関数の戻り値のデータ型, 関数の第１引数のデータ型, 関数の第２引数のデータ型, ...>

Func<int>               // function func(): int {} の関数オブジェクト
Func<boolean, int>      // function func(a: int): boolean {} の関数オブジェクト
Func<int, int, String>  // function func(a: int, b: String): int {} の関数オブジェクト
</pre>

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Func.ds" >
function func(x: int): int {
  return x * 2
}

function main() {
  var sub: Func<int, int> = func
  log ${sub(3)}
  assert(sub instanceof Func<int, int>)
  return
}

main()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Func.ds
6
true
</pre>
