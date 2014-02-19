この章では、 D-Shell の分散シェル処理機能について説明します。  

D-Shell では、複数台のホスト上でのコマンドの並列分散実行をサポートします。  
コマンドの並列実行を行うには、まずリモートホスト情報を定義します。  
定義したリモートホスト情報を用いて、対象のホストに対してコマンドを実行します。  

# リモートホスト情報の定義
***
リモートホスト情報は次のように定義します。  

<pre class="toolbar:0 highlight:0">
location シンボル名 = "ユーザ名@ホスト名[:ポート番号]"
</pre>

* シンボル名  
作成するリモートホスト情報のシンボル名を指定します。
* ユーザ名  
ログインユーザー名を指定します。
* ホスト名  
コマンドを実行するリモートホスト名を指定します。
* ポート番号  
ポート番号を指定します。

<pre class="toolbar:1 highlight:0" title="定義例">
$ location host = "hoge@192.168.8.244"
</pre>

1つのシンボルに対して複数のホストを定義することもできます。  
その場合、次のように複数のホストの情報をカンマで区切って定義します。  

<pre class="toolbar:1 highlight:0" title="定義例">
$ location hosts = "hoge@192.168.8.244, fuga@192.168.8.245:15555"
</pre>


# コマンドの実行
***
対象ホストへのコマンドは次のように実行します。  
<pre class="toolbar:0 highlight:0">
シンボル名 [timeout 時間] [trace] コマンド
</pre>

* シンボル名  
リモートホスト情報のシンボル名を指定します。
* timeout 時間(ms:ミリ秒,s:秒,m:分)  
コマンドのタイムアウトまでの時間を指定します。
* trace  
* コマンド  
リモートホスト上で実行するコマンドを指定します。

<pre class="toolbar:1 highlight:0" title="コマンド実行例">
$ location host = "hoge@192.168.8.244"
$ host import command ls
$ host ls -l /dev/null
crw-rw-rw- 1 root root 1, 3 2013-05-27 10:33 /dev/null
</pre>
リモートホスト 192.168.8.244 上で ls -l /dev/null を実行した結果が返却されます。  

<pre class="toolbar:1 highlight:0" title="コマンド実行例">
$ location hosts = "hoge@192.168.8.244, fuga@192.168.8.245"
$ hosts import command pwd
$ hosts pwd
/home/hoge
/home/fuga
</pre>
リモートホスト 192.168.8.244 と 192.168.8.245 上で pwd を実行した結果が返却されます。  


<pre class="toolbar:1 highlight:0" title="コマンド実行例">
$ location host = "hoge@192.168.8.244"
$ host import command ping
$ host timeout 10s ping 192.168.8.1
PING 192.168.8.1 (192.168.8.1) 56(84) bytes of data.
64 bytes from 192.168.8.244: icmp_seq=1 ttl=64 time=0.026 ms
64 bytes from 192.168.8.244: icmp_seq=2 ttl=64 time=0.023 ms
64 bytes from 192.168.8.244: icmp_seq=3 ttl=64 time=0.020 ms
64 bytes from 192.168.8.244: icmp_seq=4 ttl=64 time=0.020 ms
...
</pre>
リモートホスト192.168.8.244 から 192.168.8.1 へ10秒間 ping を実行します。  

