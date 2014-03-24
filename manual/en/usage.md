This chapter describes how to activate D-Shell.  

# Start-Up D-Shell
***
***D-Shell can be activated from a console by the following command.

<pre class="toolbar:1 highlight:0" title="Usage">
dshell [Option]... [Shell-Script [Argument ...]]
</pre>

* Option  
 --logging:file [File-Name]  
This option is to designate the output file for the internal command "log".  
If the file has been already designated, the output will be added to the file.  
--logging:stdout  
This option is to specify the standard output for the internal command "log".  
--logging:stderr  
This option is to specify the standard error output for the internal command "log".  
 --logging:syslog [Host IP Address]  
This option is to designate "syslog" of Host IP Address as the output destination for the internal command "log".  
If Host IP Address is not specified, it sends the output to "syslog" of a local host.  
--debug  
This option runs "dshell" in a debug mode.  
--help  
This option displays how to use the command in the standard output and ends the command.  
 --rec [REC URL]  
The execution results of the shell script  are sent to REC.  
This option is only valid in the batch mode.  
--version  
This displays the version information of the command in the standard output and ends the command.  

* Shell Script  
The file name of a shell script to run should be specified along with parameters.  
If a shell script is specified, D-Shell is activated in the batch processing mode.  
Without a shell script, D-Shell is activated in the interactive processing mode.  


#Interactive Mode & Batch Mode***
***
D-Shell can be run in both the interactive mode and the batch mode processing as other standard OS shells.  

* Interactive Mode  
When using the interactive mode, users can directly input commands from the console.  


<pre class="toolbar:0 highlight:0">
$ dshell
D-Shell, version 0.1 (Java JVM-1.7.0_45)
Copyright (c) 2013-2014, Konoha project authors
hogehoge:~> log "Hello"
Hello
</pre>

* Batch Mode  
When using the batch mode, users need to specify the file name and command options for the "dshell" command to execute a pre-scripted shell script.  

<pre class="toolbar:0 highlight:0">
$ cat test.ds
log "Hello"
$ dshell ./test.ds
Hello
</pre>
