This chapter describes the various data types that D-Shell supports.  

The following data types can be used in D-Shell.  
Users should chose a proper data type based on the scope and the type of value to be used.  

# "Boolean" Type
***
The Boolean type is the simplest data type expressing a truth value.  
The Boolean literal is either "true" or "false".  
Unlike C/C++, an integer "1" or "0" cannot be used for the Boolean value.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: Boolean.ds" >
function func() {

  var a: boolean = true
  var b = false

  log ${a}
  log ${b}

  assert(a instanceof boolean)
  assert(b instanceof boolean)

  return
}

func()
</pre>


<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Boolean.ds
true
false
</pre>


# "int" Type
***
"int" is  a data type which identifies an integer.  
Only integers can be assigned as the literal value. The signed integer type has a precision of 64 bit.  
When integers and floating-point numbers are mixed and computed, integers are automatically converted to floating-point numbers (known as "floats").  
A cast operator is used to convert floats to integers.  
 (A cast operator simply rounds off to the nearest integer.)  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: Int.ds" >
function func() {

  var a = 10
  var b = -1
  var c:int = 9223372036854775807

  log ${a}
  assert(a instanceof int)
  log ${b}
  assert(b instanceof int)
  log ${c}
  assert(c instanceof int)

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Int.ds
10
-1
9223372036854775807
</pre>

# "float" Type
***
"float" is a data type dealing with floating-point numbers. The precision is 64 bite (double precision).  
The literal of floating-point numbers can be expressed in both integers or common logarithm e.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: Float.ds" >
function func() {

  var a = 3.14
  var b: float = 0.5e3

  log ${a}
  assert(a instanceof float)
  log ${b}
  assert(b instanceof float)

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Float.ds
3.14
500.0
</pre>

# "String" Type
***
"String" is a data type which deals with a connected string of characters.  
Each character is continuously stored, thus, data manipulations like search, replace, separate can be performed easily.  
The string length is counted as the number of characters regardless of the number of bites of the character code.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: String.ds" >
function func() {

  var str = "Roll Cake"
  log ${str}
  assert(str instanceof String)

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell String.ds
Roll Cake
</pre>

The number of characters to be stored in the "String" type depends on the usable memory size (heap memory).  

## Literal and Escape of String Character
The literal of a character string can be written both in single quotes and double quotes.  
A string character in double quotes can be displayed by using an escape sequence.  
5 escape sequences are defined as follows,  

Symbol | Meaning  
---|---  
 \n      | Line Break (OS independence)  
 \t       | Tab  
 \'       | Single Quote  
 \"      | Double Quote  
 \\      | \ Symbol  


A string character in single quotes shall be displayed as written without being affected by escape sequences.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: EscSeq.ds" >
function func() {

 log"Roll \nCake"
 log'Roll \nCake'

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell EscSeq.ds
Roll
Cake
Roll\nCake
</pre>

## Development of Formula
Within a string character (or a string character equivalent) in double quotes, contents of a formula (written in a form of a string character) can be embedded with the ${Formula} format.  
To specifically prohibit the formula development, a back slash can be placed in front of "$".  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: Interpolation.ds" >
 log "Year${1900+114}"
 log "Year\${1900+114}"
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Interpolation.ds
Year2014
Year${1900+114}
</pre>


## Character String Handling
The "String" type can be handled in the same way by using the method of java.lang.String in the Java package.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: ReplaceAll.ds" >
function func() {

  var str:String = "Spring Flower"
 str.replaceAll("Flower","Roll")
  log ${str}

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell ReplaceAll.ds
Spring Roll
</pre>

# void Type
***
If the return value is "void", the return value has no meaning.  
When "void" appears in a function parameter, the function does not accept the parameter.  
When "void" appears in a return value, the function does not provide a return value.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: Void.ds" >
function func():void {

  log "function call"
  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Void.ds
function call
</pre>

# Array Type (T[]Type)
***
The "Array" type is the most basic data structure to deal with a collection of multiple objects.  
By using array classes, D-Shell can access each array for manipulation.  
Array index starts with 0.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: Array.ds" >
function func() {

  var a: int[] = [12, 34, 56, 78, 90]
  var b = ["hoge", "piyo", "fuga"]

  assert(a instanceof int[])
  assert(b instanceof String[])

  for(x in a) {
    log ${x}
  }

  for(y in b) {
    log ${y}
  }

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Array.ds
12
34
56
78
90
hoge
piyo
fuga
</pre>

# Map〈T〉 Type
***
The "Map" type is a data structure having a key and its paired value. This structure allows manipulating values using their keys.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: HashMap.ds" >
function func() {
  var map: Map<int> = { "hoge": 3, "piyo": 5, "fuga": 7 }
  for(key in map) {
    log "${key} => ${map[key]}"
  }
  assert(map instanceof Map<int>)

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell HashMap.ds
3
5
7
</pre>

# Func<T, U, V, ...> 型
***
The "Func" type is a data type which stores a defined function as a function object.  
Func objectifies a function in which a data type of T is the data type of the return value and data types of U and after are the data types of the arguments.  

<pre class="toolbar:0 highlight:0">
Func<Return-Value-Data-Type, Argument1-Data-Type,  Argument2-Data-Type, etc...>

Func<int>                 // a function object of function func(): int {} 
Func<boolean, int>     // a function object of function func(a: int): boolean {}
Func<int, int, String>  // a function object of function func(a: int, b: String): int {}
</pre>

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: Func.ds" >
function func(x: int): int {
  return x * 2
}

function main() {
  var sub: Func<int, int> = func
  log ${sub(3)}
  assert(sub instanceof Func<int, int>)
  return
}

main()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Func.ds
6
true
</pre>
