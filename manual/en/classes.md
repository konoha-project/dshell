This chapter describes how to use classes of D-Shell.  

# Class Definition
***
Classes can be defined in D-Shell in the same way as in C++ and Java.  
To define a class, member variables and member functions need to be defined as fields in the "class" statement block.  

<pre class="toolbar:0 highlight:0">
class Class-Name{
 var Field-Name1 : Data-Type1 = Initial-Value
 var Field-Name2 : Data-Type2 = Initial-Value
  ...
}
</pre>

The data type of a field must be defined.  
The member variable of a field can be a default value.  
The data type of a member function is defined by "Func" type (function object type) in which the first argument is a receiver.  

<pre class="toolbar:1 highlight:0" title="Example of definition">
class Person {
  var name: String = "naruto"
  var age: int = 17
  var isChild: Func<boolean, Person, int>
}
</pre>

Once the statement block of a  "class" is specified, the definition of a member variable is bound to the class.  

<pre class="toolbar:0 highlight:0">
// Class Definition
// Class Definition
class Person {
  var name: String = "naruto"
  var age: int = 17
  var isChild: Func<boolean, Person, int>
}

// Member Function Definition
function isChild(x: Person, a: int) : boolean {
  if (a < 5) {
    return true
  }
  return false
}
</pre>

# Object Creation
***
There are two ways to create a class object in D-Shell.  

* One is to specify the "new" operator.  
<pre class="toolbar:0 highlight:0">
var obj = new Person()
</pre>

* The other is to specify the data.  
<pre class="toolbar:0 highlight:0">
var obj = Person { name: "sakura", age: 16 }
</pre>

There are two ways to make a call to a class member function in D-Shell.  

* One is to specify the member function by placing a dot , ".",  after the class object.  

<pre class="toolbar:0 highlight:0">
var a = obj.isChild(1)
</pre>

* The other is to specify the class object for the member function parameter.  
<pre class="toolbar:0 highlight:0">
var b =isChild(obj, 6)
</pre>


<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Class.ds" >
// Class Definition
class Person {
  var name: String = "naruto"
  var age: int = 17
  var isChild: Func<boolean, Person, int>
}

// Member Function Definition
function isChild(x: Person, a: int) : boolean {
  if (a < 5) {
    return true
  }
  return false
}

function func() {
  var obj1 = new Person()
  var obj2 = Person { name: "sakura", age: 16 }

  // The member function can be specified by placing a dot , ".",  after the class object.
  log ${obj1.isChild(1)}
  // The class object can be specified for the member function parameter.
  log ${isChild(ob2, 6)}

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Class.ds
true
false
</pre>
