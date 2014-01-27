package dshell.util;

import java.io.File;
import java.util.List;

import dshell.lib.BuiltinCommandMap;
import jline.FileNameCompletor;

public class DShellFileNameCompletor extends FileNameCompletor {
	@SuppressWarnings("rawtypes")
	public int complete(final String buf, final int cursor, final List candidates) {
		String buffer = (buf == null) ? "" : buf;
		String translated = buffer;
		// special character: ~ maps to the user's home directory
		if (translated.startsWith("~" + File.separator)) {
			translated = System.getProperty("user.home") + translated.substring(1);
		}
		else if(translated.startsWith("~")) {
			translated = new File(System.getProperty("user.home")).getParentFile().getAbsolutePath();
		}
		else if(!(translated.startsWith(File.separator))) {
			translated = BuiltinCommandMap.getWorkingDirectory() + File.separator + translated;
		}
		File f = new File(translated);
		final File dir;
		if (translated.endsWith(File.separator)) {
			dir = f;
		} 
		else {
			dir = f.getParentFile();
		}
		final File[] entries = (dir == null) ? new File[0] : dir.listFiles();
		try {
			return matchFiles(buffer, translated, entries, candidates);
		}
		finally {
			// we want to output a sorted list of files
			sortFileNames(candidates);
		}
	}

}
