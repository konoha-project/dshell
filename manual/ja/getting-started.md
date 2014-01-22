この章では、 D-Shell の簡単な使い方を覚えます。  

# Hello, World
***

まずはおなじみの "Hello, World" の表示からはじめてみましょう。  

D-Shell はコマンドプロンプトから利用します。  
コンソールから "dshell" と入力して実行し、D-Shell の対話シェルを起動します。  

正常に起動すると、下のように表示されます。  

<pre class="toolbar:1" title="実行例">
$ dshell
GreenTeaScript0.1 (Reference Implementation of D-Script) on Java JVM-1.7.0_45
Copyright (c) 2013, JST/CREST DEOS and Konoha project authors
>>>
</pre>

対話シェルが起動したら、次のように入力してください。  

<pre>
println("Hello, World");
</pre>

正しく実行されれば、画面上に "Hello, World" と表示されるはずです。  

<pre class="toolbar:1" title="実行例">
>>> println("Hello, World");
Hello, World
>>>
</pre>

次にスクリプトモードでプログラムを実行してみましょう。  
D-Shell は対話シェルによるプログラムの入力以外に、あらかじめプログラムを記述したファイルを読み込むことによる実行も可能です。  

"Hello, World" を出力するプログラムをファイル "hello.ds" に保存してスクリプトファイルを作成し、"dshell" コマンドのパラメータに指定します。  

<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: hello.ds" >
println("Hello, World");
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell hello.ds
Hello, World
$ 
</pre>

これで、ひととおり D-Shell のコーディングからプログラム実行まで試すことができました。  
あとは、独自の文法やライブラリの使い方を覚えるだけで、自由にいろいろなプログラムが書けるようになります。  

