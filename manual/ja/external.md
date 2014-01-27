この章では、 D-Shell の外部コマンドの実行方法について説明します。  

# Javaクラスの利用
***
D-Shell では、プログラム内で Java のクラスを読み込み、そのクラスを利用することができます。  
クラスを利用するには import 構文で宣言します。  

<pre>
import クラス名 as シンボル名
</pre>

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: FileWrite.ds" >
import java.io.File as File;
import java.io.FileWriter as FileWriter;

function func() {

  var filewriter: FileWriter = new FileWriter(File("/tmp/file.txt"));

  filewriter.write("Hello World!");
  filewriter.close();
}

func();

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell FileWrite.ds
$ cat /tmp/file.txt
Hello World!
</pre>


# 外部コマンドの利用
***
D-Shell では、外部コマンドを呼び出し、実行することができます。  
D-Shell 上で実行できる外部コマンドは、コマンドサーチパス配下のコマンドに限られ、プログラム内であらかじめ実行するコマンドを宣言しておく必要があります。  
実行できるコマンドは、import command 構文で宣言します。  

<pre>
import command コマンド名(カンマ区切りで複数宣言可能)
</pre>

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ImportCommand.ds" >
import command echo,pwd;

echo "Hello, World."
pwd
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ImportCommand.ds
Hellom World.
/home/hogehoge
</pre>

コマンドにパラメータを渡したい場合、コマンドのあとにパラメータを記述します。  
コマンドのパラメータに変数を使用する場合、コマンドの後に $変数名(または定数名) を指定することで、変数の展開が行われます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ImportCommand.ds" >
import command ls;

function func() {
  var dir = "/dev/null";
  ls -ltr $dir
}
func();

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ImportCommand.ds
crw-rw-rw- 1 root root 1, 3 2013-05-27 10:33 /dev/null
</pre>

コマンド実行時の標準出力への出力結果は文字列として扱うことができます。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ImportCommand.ds" >
import command ls;

function func() {
  result: String = ls -ltr /tmp
  println(result);
}
func();

</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ImportCommand.ds
crw-rw-rw- 1 root root 1, 3 2013-05-27 10:33 /dev/null
</pre>

## 仕様未定
* 存在しないコマンドを指定した場合、宣言時にエラー
* コマンド実行エラーの場合、例外発生
* パイプ


# 環境変数の利用
***
D-Shell では、プログラム内で環境変数を参照することができます。  
環境変数を参照するには、import env 構文で宣言します。  
<pre>
import env 環境変数名 as シンボル名
</pre>

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル:  GetEnv.ds" >
import env HOME as home;

println(home);
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell GetEnv.ds
/home/okinoshima
</pre>

## 仕様未定
* 存在しない環境変数を指定した場合、変数参照時にエラー
* デフォルト値指定での宣言

