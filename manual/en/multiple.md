This chapter describes the D-Shell's distributed processing capability of shell programs.  

D-Shell supports parallel and distributed execution of commands on multiple hosts.  
For parallel execution, remote host information needs to be defined.  
Using the information defined for the remote hosts, commands may be executed on the hosts chosen by a user.  

# Defining Remote Host
***
Information of the remote host may be defined as follows;  

<pre class="toolbar:0 highlight:0">
location Symbol-Name = "User-Name@Host-Name[:Port-Number]"
</pre>

* Symbol-Name  
Specify a symbol name to the information to be created for a remote host.  
* User-Name  
Specify a login user name.  
* Host-Name  
Specify a name of the remote host which executes the commands.  
* Port-Number  
Specify a port number.  

<pre class="toolbar:1 highlight:0" title="Example of definition">
$ location host = "hoge@192.168.8.244"
</pre>

Note that multiple hosts may be defined by a symbol.  
To define more than one host, the information of each host should be separated by a comma.  

<pre class="toolbar:1 highlight:0" title="Example of definition">
$ location hosts = "hoge@192.168.8.244, fuga@192.168.8.245:15555"
</pre>


# Command Execution
***
A command may be executed on a specific host as follows;  
<pre class="toolbar:0 highlight:0">
Symbol-Name [timeout Time] [trace] Command
</pre>

* Symbol-Name  
Specify the symbol name of the remote host information.  
* timeout Time(ms:Milisecond,s:Second,m:Minute)  
Specify the timeout processing time of the command.  
* trace  
* Command  
Specify a command to be executed on the remote host.  

<pre class="toolbar:1 highlight:0" title="Eexample of command execution">
$ location host = "hoge@192.168.8.244"
$ host import command ls
$ host ls -l /dev/null
crw-rw-rw- 1 root root 1, 3 2013-05-27 10:33 /dev/null
</pre>
The results of executing "ls -l /dev/null" on the remote host "192.168.8.244" is returned.  

<pre class="toolbar:1 highlight:0" title="Eexample of command execution">
$ location hosts = "hoge@192.168.8.244, fuga@192.168.8.245"
$ hosts import command pwd
$ hosts pwd
/home/hoge
/home/fuga
</pre>
The results of executing "pwd" on the both remote hosts "192.168.8.244" and "192.168.8.245" are returned.  


<pre class="toolbar:1 highlight:0" title="Eexample of command execution">
$ location host = "hoge@192.168.8.244"
$ host import command ping
$ host timeout 10s ping 192.168.8.1
PING 192.168.8.1 (192.168.8.1) 56(84) bytes of data.
64 bytes from 192.168.8.244: icmp_seq=1 ttl=64 time=0.026 ms
64 bytes from 192.168.8.244: icmp_seq=2 ttl=64 time=0.023 ms
64 bytes from 192.168.8.244: icmp_seq=3 ttl=64 time=0.020 ms
64 bytes from 192.168.8.244: icmp_seq=4 ttl=64 time=0.020 ms
...
</pre>
D-Shell executes "ping" for 10 seconds from the remote host "192.168.8.244" to another remote host "192.168.8.245".  
