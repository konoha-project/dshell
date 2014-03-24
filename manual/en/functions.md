This chapter describes how to define and to use the functions.  

# User-Defined Function
***
D-Shell allows users to define functions (user-defined functions).  
Users can only define functions in the top level but cannot nest functions below (or inside) it.  
A function may be defined using syntax such as the following:  

<pre class="toolbar:0 highlight:0">
function Function-Name(Argument-Name: Argument-Data-Type, ...): Return-Value-Data-Type {
  Processing
}
</pre>

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: FunctionSample1.ds" >
function func(a: int, b: int): int {
  return a + b
}

log ${func(1, 2)}
log ${func(3, 4)}
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell FunctionSample1.ds
3
7
</pre>

Arguments and a data type of the return value may not have to be specified.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: FunctionSample2.ds" >
function func(a, b) {
  return a + b
}

log ${func(1, 2)}
log ${func(3, 4)}
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell FunctionSample2.ds
3
7
</pre>
