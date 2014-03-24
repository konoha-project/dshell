This chapter explains the basic syntax of D-Shell.  

# Entry Point
***
Unlike C or Java, D-Shell does not have a  "main" function.  
In the interactive processing mode, commands are executed in the order of their entries to the prompt.  
In the batch processing mode,  commands in the specified script file are executed in the exact order from the top.  

# Command Separator (Break of Processing)
***
D-Shell separates commands by a line break.  
Users can also separate each command by a semi-colon(;) and execute them sequentially.  
 (A semi-colon works in the same way as a line break.)  

# Use of Command Line Option
***
When executing a script, D-Shell allows users to pass a value from the command line to the program using a command line option.  
A command line option is assigned to a global variable of "ARGV: Array&lt;String&gt;" of a program.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: Commandline.ds" >
function func() {
  for(arg in ARGV) {
    log ${arg}
  }
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Commandline.ds p1 p2
Commandline.ds
p1
p2
</pre>

# Comments
***
A comment is a series of uninterpretable letters which the programs ignore.  
There are single line and multiple line comments.  

* Single-Line Comment  
<pre class="toolbar:0 highlight:0">
# It is a single-line comment as in Bash and Python.
// (Double Slashes) is a single line comment as in C++ and Java. 
</pre>

* Multiple Line Comments  
<pre class="toolbar:0 highlight:0">
/*
  Multiple lines of comments can be written as in C Language.
*/
/*
  /*
  Comments can be hierarchically nested as well.
  */
*/
</pre>

# Output to Screen
***

* Use of Internal Command ("log" Command)  
The following shows an example for a use of a "log" command built in as a D-Shell built-in command.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: OutputLog.ds" >
function func() {
  var str = "Hello, World"
  var num = 123
  log ${str}
  log ${num}
  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell OutputLog.ds
Hello, World
123
</pre>

* Use of External Command ("echo" Command)  
The following example imports an external command, "echo", into D-Shell for use.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: OutputEcho.ds" >
import command echo

function func() {
  var str = "Hello, World"
  var num = 123
  echo ${str}
  echo ${num}
  return
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell OutputEcho.ds
Hello, World
123
</pre>

* Use of Method in Java ("println" Function)  
The following example imports Java's "println" function into D-Shell for use.  

<pre class="nums:true toolbar:1 lang:scala decode:true " title="Sample code: OutputPrintln.ds" >
import java.lang.System

function func() {
  var str = "Hello, World"
  var num = 123
  System.out.println(str)
  System.out.println(num)
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell OutputPrintln.ds
Hello, World
123
</pre>

# Reserved Words
In D-Shell, there are no restrictions on the reserved words since the control structure can be redefined by syntactic sugar.  
