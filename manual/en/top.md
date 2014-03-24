In recent years, vulnerability of our society to computer system troubles has become a growing concern and dependability of the system operations is increasing in importance.  
In order to provide support for dependable performance of the system operation, we are developing a shell processing program called  "D-Shell".  
D-Shell is a shell whose main focus is to provide a capability of handling exception. D-Shell supports high-end programming language functions which have not been supported in the past.  

<pre class="toolbar:0 lang:scala decode:true">
# A lightweight programming language excellent in use for management
# A shell script which can be written in a modern programming syntax



# Capable of easily symbolizing external commands
# Capable of executing within the shell script
import command pwd

# Simple function declaration
function f() {

  # Simple variable declaration
  # Determination of data types by the type inference
  var result = ""

  # "stdout" from the execution of external commands can be treated as a character string.
  result = pwd

  # Capable of controlling the log output destinations
  log "Hello from ${result}"
}
f()
</pre>

# Documentation
Users can consult with various manuals.  
[See details](docs/)  

# Download
Users can download the package.  
[See details](download/)  

# Help
Please contact us for inquiries regarding D-Shell as follows;  
[See details](support/)  
