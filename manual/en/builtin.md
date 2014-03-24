This chapter describes D-Shell built-in commands.  

# cd
***
This command changes the current directory.  

How to use: cd [Directory-Path]  

* Directory-Path  
Users need to specify a new directory path to move to.  
If not specified, it will move to the home directory.  

<pre class="toolbar:0 highlight:0">
> pwd
hoge:/home/hogehoge/working/dshell
> cd
> pwd
hoge:/home/hogehoge
</pre>

# exit
***
This command ends the current shell.  

How to use: exit [Exit-Code]  

* Exit-Code  
Users need to specify the last code of the shell.  
If not specified, the exit code will have a return value of 0 and end the shell.  

<pre class="toolbar:0 highlight:0">
> exit
$ echo $?
0

> exit 1
$ echo $?
1
</pre>

# log
***
How to use: log [Character-String]  

* Character-String  
The character string is written as a log output.  
An output destination can be designated at startup of the shell.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Logger.ds" >
function func() {
  log "logging test"
}
func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell --logging:./test.log Logger.ds
logging test
$ cat ./test.log
Feb  3 15:39:27 WARN - logging test

$ dshell --logging:stdout Logger.ds
logging test

$ dshell --logging:stderr Logger.ds
logging test

$ dshell --logging:syslog Logger.ds
logging test
$ tail -1 /var/log/syslog
Feb  3 15:39:27 WARN - logging test
</pre>

# assert
***
If a logical expression given is false, D-Shell displays an error message and ends the shell.  
The "assert" command should be used only for debug purposes.  

How to use: assert Logical-Expression  

* Logical-Expression  
If a logical expression given is false, D-Shell displays an error message and ends the shell with the exit code 1 as a return value.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: Assert.ds" >
function func() {
  var num = 5

  assert(num == 5)
  log "assert check 1"

  assert(num instanceof int)
  log "assert check 2"

  assert(num > 2)
  log "assert check 3"
}
func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell Assert.ds
assert check 1
assert check 2
Assertion Faild
$ echo $?
1
</pre>
