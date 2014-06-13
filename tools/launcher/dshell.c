#include <stdio.h>  /* for perror */
#include <unistd.h> /* for execvp */
#include <stdlib.h> /* for EXIT_FAILURE */

#ifndef JAR_PATH
#error JAR_PATH must be defined.
#endif

#define XSTR(s) STR(s)
#define STR(s) #s
#define DSHELL_JAR_FILE XSTR(JAR_PATH) "/dshell.jar"
#define BOOT_CLASSPATH "-Xbootclasspath/a:" DSHELL_JAR_FILE

int main(int argc, char* argv[])
{
	int i, newargc = argc + 5;
	char *newargv[newargc];
	newargv[0] = "java";
	newargv[1] = BOOT_CLASSPATH;
	newargv[2] = "-XX:+TieredCompilation";
	newargv[3] = "-XX:TieredStopAtLevel=1";
	newargv[4] = "dshell.internal.main.DShell";
	for(i = 1; i < argc; i++) {
		newargv[i + 4] = argv[i];
	}
	newargv[argc + 4] = NULL;
	execvp(newargv[0], newargv);
	perror("launching dshell failed\n");
	return EXIT_FAILURE;
}
