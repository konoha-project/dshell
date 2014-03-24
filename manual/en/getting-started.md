In this chapter, you will learn how to use D-Shell in a simple way.  

# Hello, World
***
Let's start with displaying a typical greeting "Hello, World".  

D-Shell is used from the command prompt.  
From your console, key in "dshell" to activate D-Shell with an interactive mode.  

When D-Shell is correctly activated, the screen displays the following;  

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell
D-Shell, version 0.1 (Java JVM-1.7.0_45)
Copyright (c) 2013-2014, Konoha project authors
hogehoge:~>
</pre>

When D-shell is activated in an interactive mode, key in the following program;  

<pre class="toolbar:0 highlight:0">
function func() {
  log "Hello, World"
}
func()
</pre>

If the program is correctly executed,  "Hello, World" should appear on your screen.  

<pre class="toolbar:1 highlight:0" title="Example">
hogehoge:~> function func() {
              log "Hello, World"
            }
            func()
Hello, World
hogehoge:~>
</pre>

Next let's try to execute this program in a batch mode.  
D-Shell does not only allow you to type in a program from a prompt in an interactive mode, but also to read in and run a program file written in advance.  

Create a file to save a program which displays "Hello, World".  Name this script file "hello.ds" and designate it as a parameter for the "dshell" command.  

<pre class="nums:true toolbar:1 lang:scala decode:true" title="Sample code: hello.ds" >
function func() {
  log "Hello, World"
}
func()
</pre>

<pre class="toolbar:1 highlight:0" title="Example">
$ dshell hello.ds
Hello, World
$ 
</pre>

We have successfully practiced from coding to executing a simple D-Shell program.  
Now, by learning more about the unique syntax and the use of the D-Shell library, users shall be able to write a variety of programs.  
