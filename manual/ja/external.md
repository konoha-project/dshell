この章では、 D-Shell の外部コマンドの実行方法について説明します。  

# Javaクラスの利用
***
D-Shell では、プログラム内で Java のクラスを読み込み、そのクラスを利用することができます。  
クラスを利用するには import 構文で宣言します。  

<pre>
import クラス名
</pre>

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: FileWrite.ds" >
import java.io.File
import java.io.FileWriter

function func() {

  var filewriter: FileWriter = new FileWriter(File("/tmp/file.txt"))

  filewriter.write("Hello, World")
  filewriter.close()
}

func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell FileWrite.ds
$ cat /tmp/file.txt
Hello, World
</pre>


# 外部コマンドの利用
***
D-Shell では、外部コマンドを呼び出し、実行することができます。  
D-Shell 上で実行できる外部コマンドは、コマンドサーチパス配下のコマンドに限られ、あらかじめ実行するコマンドを宣言しておく必要があります。  
実行できるコマンドは、import command 構文で宣言します。  

<pre>
import command コマンド名(スペース区切りで複数宣言可能)
</pre>

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ImportCommand.ds" >
import command echo pwd

echo "Hello, World"
pwd
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ImportCommand.ds
Hello, World
/home/hogehoge
</pre>

指定したコマンドが存在しない場合、import 宣言時に異常終了します。(未定義の値)
<pre>
import command hoge   // hoge という名前のコマンドがないためエラーとなる
</pre>


コマンド実行時にパラメータを渡したい場合、コマンドのあとにパラメータを記述します。  
パラメータに変数を使用する場合、コマンドの後に $変数名(または定数名) を指定することで、変数の展開が行われます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ImportCommand.ds" >
import command ls

function func() {
  var dir = "/dev/null"
  ls -ltr $dir
}
func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ImportCommand.ds
crw-rw-rw- 1 root root 1, 3 2013-05-27 10:33 /dev/null
</pre>

コマンド実行時の標準出力への出力結果は文字列として扱うことができます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ImportCommand.ds" >
import command ls

function func() {
  var result:String = ls -ltr /dev/null
  log ${result}
}
func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ImportCommand.ds
crw-rw-rw- 1 root root 1, 3 2013-05-27 10:33 /dev/null
</pre>

コマンド実行時に bash などと同様にパイプとリダイレクトも利用できます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ImportCommand.ds" >
import command cat grep echo

echo "hoge" > hoge.txt
cat hoge.txt | grep hoge

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ImportCommand.ds
hoge
</pre>

コマンド実行結果が0以外の終了コードの場合、例外が発生します。  
コマンド実行失敗時の例外は、DShellExceptionの派生クラスとして実装されています。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Exception.ds" >

import command ls
function func() {
  try {
    // ls コマンド失敗
    ls /hoge
  } catch(e) {
    log "catch!"
  }
}
func()
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell DShellException.ds
catch!
</pre>


# 環境変数の利用
***
D-Shell では、環境変数を String 型の定数として参照することができます。  
環境変数を定数として参照するには、import env 構文で宣言します。  
<pre>
import env 環境変数名
</pre>

import 宣言後、環境変数名の定数が参照できるようになります。
<pre>
import env HOME
log HOME    // 環境変数 HOME の値が表示される(/home/hogehoge など)
</pre>

指定した環境変数が存在しない場合、定数を参照しても空文字が返ります。

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル:  GetEnv.ds" >
import env HOME               // 定義済み環境変数
import env HOGE               // 未定義の環境変数 => 値が""(空文字)の定数 HOGE が定義される

log ${HOME}  // 環境変数 HOME の値が出力される(/home/hogehoge など)
log ${HOGE}  // ""(空文字) が出力される
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell GetEnv.ds
/home/hogehoge

</pre>
