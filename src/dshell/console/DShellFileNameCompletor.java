package dshell.console;

import java.io.File;
import java.util.List;

import dshell.lib.RuntimeContext;
import dshell.lib.Utils;
import jline.FileNameCompletor;

public class DShellFileNameCompletor extends FileNameCompletor {
	@SuppressWarnings("rawtypes")
	@Override
	public int complete(final String buf, final int cursor, final List candidates) {
		String buffer = (buf == null) ? "" : buf;
		String translated = buffer;
		if(translated.startsWith("~" + File.separator)) {
			translated = Utils.getEnv("HOME") + translated.substring(1);
		}
		else if(translated.startsWith("~")) {
			translated = new File(Utils.getEnv("HOME")).getParentFile().getAbsolutePath();
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
			return this.matchFiles(buffer, translated, entries, candidates);
		}
		finally {
			this.sortFileNames(candidates);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public int matchFiles(final String buffer, final String translated, final File[] entries, final List candidates) {
		if(entries == null) {
			return -1;
		}
		for(int i = 0; i < entries.length; i++) {
			if(entries[i].getPath().startsWith(translated)) {
				String fileNameSuffix = (entries[i].isDirectory()) ? File.separator : "";
				String fileName = entries[i].getName() + fileNameSuffix;
				candidates.add(fileName);
			}
		}
		int index = buffer.lastIndexOf(File.separator);
		return index + File.separator.length();
	}
}
