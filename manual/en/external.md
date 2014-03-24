This chapter describes how to execute external commands of D-Shell.  

# Utilization of Java Class
***
D-Shell can read and utilize Java classes.  
To use Java classes, users need to declare in the "import" command statement.  

<pre class="toolbar:0 highlight:0">
import Class-Name
</pre>

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: FileWrite.ds" >
import java.io.File
import java.io.FileWriter

function func() {

  var filewriter: FileWriter = new FileWriter(new File("/tmp/file.txt"))

  filewriter.write("Hello, World")
  filewriter.close()
}

func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell FileWrite.ds
$ cat /tmp/file.txt
Hello, World
</pre>


# Use of External Command
***
In the D-Shell program, external commands can be called and executed.  
External commands users can run on D-Shell are limited to the commands under the command search path. Thus, those commands need to be declared beforehand.  
Users need to specify the necessary commands in the "import command" statement for execution.  

<pre class="toolbar:0 highlight:0">
import command Command-Name (Multiple commands can be declared by each being separated with a space)
</pre>

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: ImportCommand.ds" >
import command echo pwd

echo "Hello, World"
pwd
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell ImportCommand.ds
Hello, World
/home/hogehoge
</pre>

If the specified command is not found, the program ends with ABND at the "import" declaration. (undefined value)  
<pre class="toolbar:0 highlight:0">
import command hoge   // The "hoge" command does not exist and it becomes an error.
</pre>


When users want to pass a parameter for the command execution, the parameter should be stated after the command.  
When the variables are used for a parameter, users should specify "$Variable-Name or (Constant-Name)" after the command. Variable expansion will be performed.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: ImportCommand.ds" >
import command ls

function func() {
  var dir = "/dev/null"
  ls -ltr $dir
}
func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell ImportCommand.ds
crw-rw-rw- 1 root root 1, 3 2013-05-27 10:33 /dev/null
</pre>

The results to the standard output of the command execution can be treated as a character string.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: ImportCommand.ds" >
import command ls

function func() {
  var result:String = ls -ltr /dev/null
  log ${result}
}
func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell ImportCommand.ds
crw-rw-rw- 1 root root 1, 3 2013-05-27 10:33 /dev/null
</pre>

"pipe" and "redirect" can be used at the command execution in the same way as "bash" and others.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: ImportCommand.ds" >
import command cat grep echo

echo "hoge" > hoge.txt
cat hoge.txt | grep hoge

</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell ImportCommand.ds
hoge
</pre>

If the end code of the command execution result is other than 0, an exception has been identified.  
Exceptions for command execution failure are implemented as a derived class of DShellException.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Exception.ds" >

import command ls
function func() {
  try {
    // ls　Command Failure
    ls /hoge
  } catch(e) {
    log "catch!"
  }
}
func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell DShellException.ds
catch!
</pre>


# Use of Environment Variables
***
In D-Shell, users can reference environment variables as the String type constant.  
 To reference an environment variable as a constant, users need to declare that in the  "import env" statement.  
<pre class="toolbar:0 highlight:0">
import env Environment-Variable-Name
</pre>

After declaring "import", users become able to reference the constant with the environment variable name.  
<pre class="toolbar:0 highlight:0">
import env HOME
log ${HOME}    //   A value of the environtment variable "HOME" will be displayed. (/home/hogehoge, etc)
</pre>

When the specified environment variable is not found, referencing the constant will result in a return of a null string.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code:  GetEnv.ds" >
import env HOME               //  The environment variable is predefined. 
import env HOGE              //   The environment variable is not predefined  =>   The constant "HOGE" with the value of  "" (a null string) is defined.

log ${HOME}  // D-Shell outputs the value of the environment variable HOME. (/home/hogehoge, etc)
log ${HOGE}  // D-Shell outputs ""(a null string).
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell GetEnv.ds
/home/hogehoge

</pre>
