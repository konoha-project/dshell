This chapter describes the control structure of D-Shell.  

# if / else / else if
***

## if

The "if" statement is the simplest conditional brunching.  
When the logical expression of an "if" statement is "true", D-Shell executes the commands written in the statement block.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: If.ds" >
function func(num) {
  if (num == 2) {
    log "if block: ${num}"
  }
  return
}

func(1)
func(2)
func(3)
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell If.ds
if block: 2
</pre>

## if else

When the logical expression of an "if" statement is "false", D-Shell executes the commands written in the "else" statement block.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Else.ds" >
function func(num) {
  if (num > 2) {
    log "if block: ${num}"
  }
  else {
    log "else block: ${num}"
  }
  return
}

func(1)
func(2)
func(3)
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Else.ds
else block: 1
else block: 2
if block: 3
</pre>

## else if

When the logical expression of an "if" statement is "false", but the logical expression of an " else if" statement is "true", D-Shell executes the commands written in the statement block.  
The "else if" statement allows users to use multiple conditional brunches to add statement blocks.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: ElseIf.ds" >
function func(num) {
  if(num == 1) {
    log "if block: ${num}"
  }
  else if(num == 2) {
    log "else if block: ${num}"
  }
  else {
    log "else block: ${num}"
  }
  return
}

func(1)
func(2)
func(3)
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell ElseIf.ds
if block: 1
else if block: 2
else block: 3
</pre>

# while
***
The "while" loop works such that as long as the logical expression of a "while" statement holds "true", D-Shell repeats executing the commands written in the statement block.  
It works in the same way as a "while" loop in C/C++.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: While.ds" >
function func() {
  var num = 1
  while (num < 3) {
    log "while block: ${num}"
    num = num + 1
  }
  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell While.ds
while block: 1
while block: 2
</pre>

# for
***
The "for" loop works such that for each element of a data array or a data structure corresponding to an associative array (a hash) , D-Shell repeats executing the commands written in the statement block.  
If an object is an arrray, a value is substituted for variables. If an object is an associative array, a key is substituted for variables.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Foreach.ds" >
function func() {
  var arr: int[] = [1, 2, 3, 5, 7]
  var map: Map<int> = {"a": 1, "b": 2, "c": 3 }

 # For an array
  for(val in arr) {
    log ${val}
  }
 # For a data structure corrsponding to an associative array　（Hash)
  for(key in map) {
    log "${key} => ${map[key]}"
  }
  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Foreach.ds
1
2
3
5
7
a => 1
b => 2
c => 3
</pre>

# break
***
The "break" ends execution of the current "while" loop .  
Users can exit from the statement block in a loop structure with a "break" statement.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Break.ds" >
function func() {
  var num = 1
  while (true) {
    log ${num}
    num = num + 1
    if(num == 3) {
      break
    }
  }
  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Break.ds
1
2
</pre>

# return
***
The "return" statement is used to end execution of the current function and to return the control to the caller.  
If a value is specified after the "return", the value can be returned as the return value.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Return.ds" >
function sub() {
  return "sub call"
}

log ${sub()}
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Return.ds
sub call
</pre>

# try / catch / finally (Exception Processing)
***
When exceptions occur during processing, D-Shell allows users to enclose the code with the "try" block to capture the exceptions.  
There needs to be a corresponding "catch" block  in each "try" block.  
In order to capture different types of exceptions, multiple "catch" blocks may be used.  
Users can also specify the "finally" block after the "catch" blocks.  
The codes written in the "finally" block shall always be executed after the "try" and the "catch" blocks.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Exception.ds" >
function raise(num) {
  if(num == 1) {
    log "throw: DShellException"
    throw new DShellException("1")
  } else if(num == 2) {
    log "throw: NullException"
    throw new NullException("2")
  } else {
    log "other"
  }
  return
}

function func(num) {
  log "func call"
  try {
    log "try"
    raise(num)

  } catch(e: DShellException) {
    log "catch: DShellException"
    log "error message: ${e.getErrorMessage()}"
  } catch(e: NullException) {
    log "catch: NullException"
    log "error message: ${e.getErrorMessage()}"
  } finally {
    log "finaly"
  }
  return
}

func(1)
func(2)
func(3)
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Exception.ds
func call
try
throw: DShellException
catch: DShellException
error message: 1
finaly
func call
try
throw: NullException
catch: NullException
error message: 2
finaly
func call
try
finaly
</pre>
