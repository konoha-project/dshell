クラスとオブジェクト

この章では、 D-Shell のクラスの利用方法を説明します。

# クラス定義
***
D-Shellでは、C++ や Java と同様にクラスを定義することができます。  
クラスを定義するには、class ステートメントブロックにメンバ変数やメンバ関数をフィールドとして定義します。  

<pre>
class クラス名 {
  field フィールド名1 : データ型1;
  field フィールド名2 : データ型2;
  ...
}
</pre>

フィールドのデータ型の定義は必須です。  
メンバ関数のデータ型は 第１引数をレシーバとするFunc 型(関数オブジェクト型)で定義します。  

<pre class="toolbar:1" title="定義例">
class Person {
  field name : String;
  field age : int;
  field isChild : Func<boolean, this, int>;
}
</pre>

class ステートメントブロックのあとにメンバ関数を定義すると、クラスにバインドされます。

<pre>
// クラス定義
class Person {
  field name : String;
  field age : int;
  field isChild : Func<boolean, this, int>;
}

// メンバ関数定義
function isChild(x:Person, a:int) :boolean { return (a > 5)? true : false; }
</pre>

# オブジェクト生成
***
クラスのオブジェクトの生成には new 演算子を使用します。  
<pre>
var obj = new Person();
</pre>

クラスのメンバ関数の呼び出しには、二つの方法があります。  

* クラスオブジェクトの後に「ドット」付けてメンバ関数を指定する  

<pre>
var a = obj.isChild();
</pre>

* メンバ関数のパラメータにクラスオブジェクトを指定する  
<pre>
var b =isChild(obj);
</pre>


<pre class="nums:true toolbar:1 plain:true lang:scala highlight:0 decode:true " title="サンプル: Class.ds" >
// クラス定義
class Person {
  field name : String;
  field age : int;
  field isChild : Func<boolean, this, int>;
}

// メンバ関数定義
function isChild(x:Person, a:int) :boolean { return (a > 5)? true : false; }

func() {
  var obj = new Person();
  // クラスオブジェクトの後に「ドット」付けてメンバ関数を指定
  println(obj.isChild(6));
  // メンバ関数のパラメータにクラスオブジェクトを指定
  println(isChild(obj, 1));
}

func();
</pre>

<pre class="toolbar:1" title="実行例">
$ dshell Class.ds
true
false
</pre>


## 仕様未定

* メンバ変数へのアクセス

