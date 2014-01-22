この章では、 D-Shell の外部コマンドの実行方法について説明します。  

# Javaクラスの利用
***
D-Shell では、プログラム内で Java のクラスを読み込み、そのクラスを利用することができます。  
クラスを利用するには次のような構文で宣言します。  

import クラス名 as シンボル名

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
D-Shell では、通常の Unix シェルと同じようにコマンドを入力するとそれに応じた処理が実行されたり、シェルスクリプトファイルからコマンド群を読み込み実行することもできます。  
D-Shell 上で実行できるコマンドは、コマンドサーチパス配下のコマンドに限られ、あらかじめ実行するコマンドを宣言しておく必要があります。  
実行できるコマンドは次のような構文で宣言します。  

import command コマンド名(カンマ区切りで複数宣言可能)

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: ImportCommand.ds" >
import command echo,pwd;

echo "Hello World!"
pwd
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell ImportCommand.ds
Hello World!
/home/hogehoge
</pre>

コマンドにパラメータを渡したい場合、コマンドの後にパラメータを記述します。  
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

* 存在しないコマンドを指定した場合、宣言時にエラー
* コマンド実行エラーの場合、例外発生
* パイプ

# 環境変数の利用
***
D-Shell では、プログラム内で環境変数を参照することができます。  
環境変数を参照するには次のような構文で宣言します。  

import env 環境変数名 as シンボル名

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル:  GetEnv.ds" >
import env HOME as home;

println(home);
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell GetEnv.ds
/home/okinoshima
</pre>

* 存在しない環境変数を指定した場合、変数参照時にエラー
* 宣言時にデフォルト値を指定した場合、シンボル名にデフォルト値が設定される(宣言方法未定)

