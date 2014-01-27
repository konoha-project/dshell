この章では、 D-Shell の制御構造について説明します。  

# if / else / else if
***

## if

if 構文は最も単純な条件分岐です。  
if 構文の論理式が true の場合、ステートメントブロックに記述された命令を実行します。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: If.ds" >
function func(x: num):void {
  if (num == 2) {
    println(num);
  }
}

func(1);
func(2);
func(3);

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell If.ds
2
</pre>

## if else

if 構文に続く論理式が false の場合、else ステートメントブロックに記述された命令を実行します。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Else.ds" >
function func(num):int {
  if (num > 2) {
    return num;
  }
  else {
    return -1;
  }
}

println(func(1));
println(func(2));
println(func(3));

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Else.ds
1
2
-1
</pre>

## else if

if 構文の論理式が false かつ、else if 構文の論理式が true の場合、ステートメントブロックに記述された命令を実行します。  
else if 構文では単独の条件分岐だけではなく複数の条件分岐を使ってステートメントブロックを追加することができます。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ElseIf.ds" >
function func(num: int): String {
  if(num == 1) {
    return "hoge";
  }
  else if(num == 2) {
    return "piyo";
  }
  else {
    return "fuga";
  }
}

println(func(1));
println(func(2));
println(func(3));

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ElseIf.ds
hoge
piyo
fuga
</pre>

# while
***
while ループは、while 構文に続く論理式が true の間、ステートメントブロックに記述された命令を繰り返します。  
C/C++ の while ループと同様の動作をします。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: While.ds" >
function func() {
  var num = 1;
  while (num < 3) {
    println(num);
    num = num + 1;
  }
}

func();

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell While.ds
1
2
</pre>

# break
***
break は、現在実行中の while 構造の実行を終了します。  
break 文を使うと、現在繰り返しているループ構造のステートメントブロックから抜け出すことができます。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Break.ds" >
function func() {
  var num = 1;
  while (num) {
    println(num);
    num = num + 1;
    if(num == 3) {
      break;
    }
  }
}

func();

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
function sub(): String {
  return "sub call!"
}

println(sub());

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Return.ds
sub call!
</pre>

# try / catch / finally (例外処理)
***
実行中の処理に例外が発生した場合、発生した例外を捕捉するには、コードを try ブロックで囲みます。  
各 try ブロックには、対応する catch ブロックが存在する必要があります。  
異なる型の例外を捕捉するために複数の catch ブロックを使用することができます。  
また、catch ブロックの後に finally ブロックも指定できます。  
finally ブロックに書いたコードは、try および catch ブロックの後で常に実行されます。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Exception.ds" >
command ls;

void func(String dir) {
  println("func call!");
  try {
    println("try");

    // TODO: dirで例外が発生する処理
    ls -l $dir

  } catch(DShellException1 e) {
    println("catch1");
    
  } catch(DShellException2 e) {
    println("catch2");
    
  } finally {
    println("finaly");
    
  }
}

func("no exception");
func("exception1");
func("exception2");

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Exception.ds
func call!
try
finaly
func call!
try
catch1
finaly
func call!
try
catch2
finaly

</pre>

