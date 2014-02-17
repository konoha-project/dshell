package dshell.util;

import java.io.File;
import java.util.List;

import dshell.lib.RuntimeContext;
import jline.FileNameCompletor;

public class DShellFileNameCompletor extends FileNameCompletor {
	@SuppressWarnings("rawtypes")
	@Override
	public int complete(final String buf, final int cursor, final List candidates) {
		String buffer = (buf == null) ? "" : buf;
		String translated = buffer;
		if(translated.startsWith("~" + File.separator)) {
			translated = System.getProperty("user.home") + translated.substring(1);
		}
		else if(translated.startsWith("~")) {
			translated = new File(System.getProperty("user.home")).getParentFile().getAbsolutePath();
		}
		else if(!translated.startsWith(File.separator)) {
			String workingDir = RuntimeContext.getContext().getWorkingDirectory();
			if(workingDir.equals("/")) {
				workingDir = "";
			}
			translated = workingDir + File.separator + translated;
		}
		File file = new File(translated);
		final File dir;
		if(translated.endsWith(File.separator)) {
			dir = file;
		} 
		else {
			dir = file.getParentFile();
		}
		final File[] entries = (dir == null) ? new File[0] : dir.listFiles();
		try {
			return matchFiles(buffer, translated, entries, candidates);
		}
		finally {
			sortFileNames(candidates);
		}
	}
}
