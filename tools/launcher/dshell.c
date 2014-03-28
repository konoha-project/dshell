#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>

#define CHAR_SIZE 32
#define PATH_SIZE 512


static char *getJarPath(char *progPath)
{
	char jarName[] = "dshell.jar";
	char *path = (char *)malloc(sizeof(char) * PATH_SIZE);
#ifdef JAR_PATH

#define XSTR(s) STR(s)
#define STR(s) #s
	snprintf(path, PATH_SIZE, "%s/%s", XSTR(JAR_PATH), jarName);
#else
	int endOfPrefix = 0;
	int i = 0;
	while(progPath[i] != '\0') {
		if(progPath[i] == '/') {
			endOfPrefix = i;
		}
		i++;
	}
	char *prefix =  (char *)malloc(sizeof(char) * (endOfPrefix + 1));
	strncpy(prefix, progPath, endOfPrefix);
	prefix[endOfPrefix] = '\0';
	snprintf(path, PATH_SIZE, "%s/%s", prefix, jarName);
#endif
	return path;
}


int main(int argc, char **argv)
{
	int size = argc + 3;
	char **const params = (char **)malloc(sizeof(char *) * size);
	int i;
	for(i = 0; i < size; i++) {
		if(i == 0) {
			params[i] = (char *)malloc(sizeof(char) * CHAR_SIZE);
			strncpy(params[i], "java", CHAR_SIZE);
		} else if(i == 1) {
			params[i] = (char *)malloc(sizeof(char) * CHAR_SIZE);
			strncpy(params[i], "-jar", CHAR_SIZE);
		} else if(i == 2) {
			params[i] = (char *)malloc(sizeof(char) * PATH_SIZE);
			strncpy(params[i], getJarPath(argv[0]), PATH_SIZE);
		} else if(i == size - 1) {
			params[i] = NULL;
		} else {
			char *arg = argv[i - 2];
			int argSize = strlen(arg) + 1;
			params[i] = (char *)malloc(sizeof(char) * argSize);
			strncpy(params[i], arg, argSize);
		}
	}
	execvp(params[0], params);
	perror("launching dshell failed\n");
	return -1;
}
