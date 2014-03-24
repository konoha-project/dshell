This chapter describes how to install D-Shell.  

# Installation onto Linux System
***
## Verification & Preparation for Hardware Requirements
Following are the hardware requirements for D-Shell;  

* Required Software  
JRE(Java Runtime Environment) 7  

## Installation from Package
***
### Debian-Based System
Download the target system package from the D-Shell site.  

<pre class="toolbar:0 highlight:0">
$ wget xxx dshell-0.1-1.x86_64.deb
</pre>

Use "dpkg" command to install the downloaded package.  

<pre class="toolbar:0 highlight:0">
$ sudo dpkg -i ./dshell-0.1-1.x86_64.deb
$ dshell
</pre>


### Redhat-Based System
Download the target system package from the D-Shell site.  

<pre class="toolbar:0 highlight:0">
$ wget xxx dshell-0.1-1.x86_64.rpm
</pre>

Use "rpm" command to install the downloaded package.  

<pre class="toolbar:0 highlight:0">
$ sudo rpm -ivh dshell-0.1-1.x86_64.rpm
$ dshell
</pre>

The system will be installed in /usr/local/bin by default.  

## Installation from Source
***
Acquire Git repository of D-Shell.  

<pre class="toolbar:0 highlight:0">
$ git clone https://github.com/konoha-project/dshell
</pre>

Use "make" and "ant" commands to make D-Shell.  
For INSTALL_PREFIX, designate a directory where you want to install D-Shell.  
Otherwise, it will be installed in the home directory by default.  

<pre class="toolbar:0 highlight:0">
$ cd dshell
$ make
$ make install INSTALL_PREFIX=/usr/local
$ dshell
</pre>
