#include <stdio.h>  /* for perror */
#include <unistd.h> /* for execvp */
#include <stdlib.h> /* for EXIT_FAILURE */

#ifndef JAR_PATH
#error JAR_PATH must be defined.
#endif

#define XSTR(s) STR(s)
#define STR(s) #s
#define DSHELL_JAR_FILE XSTR(JAR_PATH) "/dshell.jar"

int main(int argc, char* argv[])
{
	int i, newargc = argc + 3;
	char *newargv[newargc];
	newargv[0] = "java";
	newargv[1] = "-jar";
	newargv[2] = DSHELL_JAR_FILE;
	for(i = 1; i < argc; i++) {
		newargv[i + 2] = argv[i];
	}
	newargv[argc + 2] = NULL;
	execvp(newargv[0], newargv);
	perror("launching dshell failed\n");
	return EXIT_FAILURE;
}
