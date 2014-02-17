この章では、 D-Shell の制御構造について説明します。  

# if / else / else if
***

## if

if 構文は最も単純な条件分岐です。  
if 構文の論理式が true の場合、ステートメントブロックに記述された命令を実行します。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: If.ds" >
function func(num) {
  if (num == 2) {
    log "if block: ${num()}"
  }
  return
}

func(1)
func(2)
func(3)
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell If.ds
if block: 2
</pre>

## if else

if 構文に続く論理式が false の場合、else ステートメントブロックに記述された命令を実行します。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Else.ds" >
function func(num) {
  if (num > 2) {
    log "if block: ${num()}"
  }
  else {
    log "else block: ${num()}"
  }
  return
}

func(1)
func(2)
func(3)
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Else.ds
else block: 1
else block: 2
if block: 3
</pre>

## else if

if 構文の論理式が false かつ、else if 構文の論理式が true の場合、ステートメントブロックに記述された命令を実行します。  
else if 構文では単独の条件分岐だけではなく複数の条件分岐を使ってステートメントブロックを追加することができます。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ElseIf.ds" >
function func(num) {
  if(num == 1) {
    log "if block: ${num()}"
  }
  else if(num == 2) {
    log "else if block: ${num()}"
  }
  else {
    log "else block: ${num()}"
  }
  return
}

func(1)
func(2)
func(3)
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ElseIf.ds
if block: 1
else if block: 2
else block: 3
</pre>

# while
***
while ループは、while 構文に続く論理式が true の間、ステートメントブロックに記述された命令を繰り返します。  
C/C++ の while ループと同様の動作をします。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: While.ds" >
function func() {
  var num = 1
  while (num < 3) {
    log "while block: ${num}"
    num = num + 1
  }
  return
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell While.ds
while block: 1
while block: 2
</pre>

# for
***
for ループは、配列や連想配列のデータ構造の各要素に対して、ステートメントブロックに記述された命令を繰り返します。  
対象となるオブジェクトが配列の場合は変数に値を代入し、連想配列の場合は変数にキーが代入されます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Foreach.ds" >
function func() {
  var arr: int[] = [1, 2, 3, 5, 7]
  var map: Map<int> = {"a": 1, "b": 2, "c": 3 }

  # 配列の場合
  for(val in arr) {
    log ${val}
  }
  # 連想配列の場合
  for(key in map) {
    log "${key} => ${map[key]}"
  }
  return
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Foreach.ds
1
2
3
5
7
a => 1
b => 2
c => 3
</pre>

# break
***
break は、現在実行中の while 構造の実行を終了します。  
break 文を使うと、現在繰り返しているループ構造のステートメントブロックから抜け出すことができます。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Break.ds" >
function func() {
  var num = 1
  while (true) {
    log ${num()}
    num = num + 1
    if(num == 3) {
      break
    }
  }
  return
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Break.ds
1
2
</pre>

# return
***
return 構文は実行中の関数を終了し、制御をその呼び出し元に返すために使用されます。  
return の後に値を指定すると戻り値として返却することができます。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Return.ds" >
function sub() {
  return "sub call"
}

log ${sub()}
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Return.ds
sub call
</pre>

# try / catch / finally (例外処理)
***
実行中の処理に例外が発生した場合、発生した例外を捕捉するには、コードを try ブロックで囲みます。  
各 try ブロックには、対応する catch ブロックが存在する必要があります。  
異なる型の例外を捕捉するために複数の catch ブロックを使用することができます。  
また、catch ブロックの後に finally ブロックも指定できます。  
finally ブロックに書いたコードは、try および catch ブロックの後で常に実行されます。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Exception.ds" >
function raise(num) {
  if(num == 1) {
    log "throw: DShellException"
    throw new DShellException("1")
  } else if(num == 2) {
    log "throw: NullException"
    throw new NullException("2")
  } else {
    log "other"
  }
  return
}

function func(num) {
  log "func call"
  try {
    log "try"
    raise(num)

  } catch(e: DShellException) {
    log "catch: DShellException"
    log "error message: ${e.getErrorMessage()}"
  } catch(e: NullException) {
    log "catch: NullException"
    log "error message: ${e.getErrorMessage()}"
  } finally {
    log "finaly"
  }
  return
}

func(1)
func(2)
func(3)
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Exception.ds
func call
try
throw: DShellException
catch: DShellException
error message: 1
finaly
func call
try
throw: NullException
catch: NullException
error message: 2
finaly
func call
try
finaly
</pre>
