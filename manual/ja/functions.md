この章では、 関数の定義と使い方について説明します。  

# ユーザー定義関数
***
D-Shell では、関数(ユーザ定義関数)を定義することができます。  
関数を定義できるのはトップレベルのみで、関数のネストはできません。  
関数は function 構文で定義します。  

<pre class="toolbar:0 highlight:0">
function 関数名(引数名: 引数のデータ型, ...): 戻り値のデータ型 {
  処理
}
</pre>

<pre class="nums:true toolbar:1 lang:scala decode:true" title="サンプル: FunctionSample1.ds" >
function func(a: int, b: int): int {
  return a + b
}

log ${func(1, 2)}
log ${func(3, 4)}
</pre>

<pre class="toolbar:1 highlight:0" title="実行例">
$ dshell FunctionSample1.ds
3
7
</pre>

引数と戻り値のデータ型は省略可能です。  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="サンプル: FunctionSample2.ds" >
function func(a, b) {
  return a + b
}

log ${func(1, 2)}
log ${func(3, 4)}
</pre>

<pre class="toolbar:1 highlight:0" title="実行例">
$ dshell FunctionSample2.ds
3
7
</pre>
