package dshell;

import java.io.File;

public class Utils {
	public final static boolean isUnixCommand(String cmd) {
		String[] path = System.getenv("PATH").split(":");
		for(int i = 0; i < path.length; i++) {
			if(isFileExists(path[i] + "/" + cmd)) {
				return true;
			}
		}
		return false;
	}

	public final static boolean isFile(String Path) {
		return new File(Path).isFile();
	}

	public final static boolean isDirectory(String Path) {
		return new File(Path).isDirectory();
	}

	public final static boolean isFileExists(String Path) {
		return new File(Path).exists();
	}

	public final static boolean isFileReadable(String Path) {
		return new File(Path).canRead();
	}

	public final static boolean isFileWritable(String Path) {
		return new File(Path).canWrite();
	}

	public final static boolean isFileExecutable(String Path) {
		return new File(Path).canExecute();
	}

	//flag operator
	public final static boolean is(int option, int flag) {
		option &= flag;
		return option == flag;
	}

	public final static int setFlag(int option, int flag, boolean set) {
		if(set && !is(option, flag)) {
			return option | flag;
		}
		else if(!set && is(option, flag)) {
			return option & ~flag;
		}
		return option;
	}
}
