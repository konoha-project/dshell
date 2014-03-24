This chapter describes how to use variables and constants in D-Shell.  

# Variables
***
When declaring a variable in D-Shell, users need to specify the variable name, the data type and the initial value.  
A variable is declared in the following syntax;  

var Variable-Name: Data-Type = Initial-Value  

* Variable Declaration and Scope of Declaration  
Variables can only be declared in a function. The scope of the variables is inside the function.  

* Data Type Specification and Type Inference  
If you do not specify a data type for variable declaration, D-Shell automatically assigns a data type based on the data type of the initial value. (Type Inference)  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Variable.ds" >
function func() {
  var age: int = 17
  var name = "uzumaki naruto"

  log ${age}
  log ${name}

  return
}

func()

// "age" and "name" can not be referred to outside functions.
// Reference of an undefined log age (err)
// Reference of an undefined log name (err)
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Variable.ds
17
uzumaki naruto
</pre>

# Cast
***

* Type Cast  
A cast operator is used to convert　a data type to an another specific data type.  
A cast operator is used in the following syntax;  

<pre class="toolbar:0 highlight:0">
(Data-Type) Variable-Name
</pre>

* Instance Determination  
The "instanceof" operator is used to determine in what data type the object is produced.  
The "instanceof" operator is used in the following syntax;  

<pre class="toolbar:0 highlight:0">
Variable-Name instanceof Data-Type
</pre>

If the variable data type matches with the data type specified by a user,  D-Shell returns "true".  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: TypeCast.ds" >
function func() {
  var a: int = 123
  var b: float = 2.0
  var c: String = "45.67"

  // int -> float
  var d = (float)a

  // String -> int
  var e = (int)c

  // float -> String
  var f = (String)b

  assert(d instanceof float)
  assert(e instanceof int)
  assert(f instanceof String)

  log ${d}
  log ${e}
  log ${f}

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell TypeCast.ds
123.0
45.67
2.0
</pre>

# Constants
***
When declaring a constant in D-Shell, users need to specify the constant name and its setting value.  
A constant is declared in the following syntax;  

<pre class="toolbar:0 highlight:0">
let Constant-Name = Setting-Value
</pre>

* Constant Declaration and Scope of Declaration  
If constants are declared inside a function, their scope is limited to a local block scope. If they are declared outside functions, their scope has a global scope.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Constant.ds" >
let color ="red"

function func() {
  let color = "green"
  log "local color: ${color}"
  return
}

func()

log "top color: ${color}"
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Constant.ds
local color: green
top color: red
</pre>
