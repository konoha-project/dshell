This chapter describes D-Shell's operators.  

# Algebraic Operators
***
Algebraic operators are used to add, subtract, multiply and divide numerical values.  

Examples | Processing  
--|--  
 - A       | negation  
 A + B    | addition  
 A - B    | subtraction  
 A * B    | multiplication  
 A / B    | division  
 A % B    | modulus (remainder of A divided by B)  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code:  AlgebraicOp.ds" >
function func() {
  var a = 4
  var b = 2

  log ${-a}
  log ${a + b}
  log ${a - b}
  log ${a * b}
  log ${a / b}
  log ${a % b}

  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell AlgebraicOp.ds
-4
6
2
8
2
0
</pre>

# Comparison Operators
***
Comparison operators are used to compare two numerical values:  to see if they are the same or if not, then, the relational expressions of two compared numerical values.  
Each operator returns a Boolean type value.  

Examples | Processing  
--|--  
A == B    | identical  
A != B     | not equal  
A < B      | less than  
A > B      | greater than  
A <= B    | less than or equal to  
A >= B    | greater than or equal to  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code:  RelationalOp.ds" >
function func() {
  var a = 4
  var b = 2
  var c = 2

  log ${a == c}
  log ${a != b}
  log ${b < a}
  log ${a > b}
  log ${a <= b}
  log ${b >= c}
  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell RelationalOp.ds
true
true
true
true
true
true
</pre>


# Logical Operators
***
Logical operators are used to show complicated conditional expressions with multiple conditions combined.  
Each operator returns a Boolean type value.  

Examples  | Name      | Processing and Results  
--|--  
! A          |logical NOT| if A is not true, then true  
A && B    |logical AND| if both A and B are true, then true  
A || B    | OR         | if A or B is true, then true  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: LogicalOp.ds" >
function func() {
  var a = 4
  var b = 2

  log ${!(a == b)}
  log ${(a > b) && (a >= b)}
  log ${(a > b) || (a == b)}
  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell LogicalOp.ds
true
true
true
</pre>

# Pattern Matching Operator
***
A pattern matching operator is used to show the regular conditional expression.  
Examples  | Name      | Processing and Results  
--|--  
A =~ B     |regular expression| The character string A satisfies a standard expression pattern B, then true  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Regex.ds" >
function func() {
  var str = "abc"
  if (str =~ "^a") {
    log "match!"
  }
  else {
    log "not match!"
  }
  if (str =~ "^b") {
    log "match!"
  }
  else {
    log "not match!"
  }
  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Regex.ds
match!
not match!
</pre>
