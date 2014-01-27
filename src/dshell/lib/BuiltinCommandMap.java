package dshell.lib;

import java.util.ArrayList;

import dshell.lib.ProcOption.MergeType;
import dshell.lib.ProcOption.OutputType;
import dshell.lib.ProcOption.ProcPosition;

interface CLibraryWrapper extends com.sun.jna.Library {
	CLibraryWrapper INSTANCE = (CLibraryWrapper) com.sun.jna.Native.loadLibrary("c", CLibraryWrapper.class);
	
	int chdir(String path);
	//char *getcwd(char *buf, size_t size);
	String getcwd(byte[] buf, int size);
}

public class BuiltinCommandMap {
	private static enum BuiltinCommandSymbol {
		cd,
		exit;
	}

	public static ArrayList<String> getCommandSymbolList() {
		ArrayList<String> symbolList = new ArrayList<String>();
		for(BuiltinCommandSymbol symbol : BuiltinCommandSymbol.values()) {
			symbolList.add(symbol.name());
		}
		return symbolList;
	}

	public static BuiltinCommand createCommand(ArrayList<String> cmds) {
		BuiltinCommand command = null;
		if(BuiltinCommandSymbol.cd.name().equals(cmds.get(0))) {
			command = new Command_cd();
			command.setArgumentList(cmds);
		}
		else if(BuiltinCommandSymbol.exit.name().equals(cmds.get(0))) {
			command = new Command_exit();
			command.setArgumentList(cmds);
		}
		return command;
	}

	public static int changeDirectory(String path) {
		if(path.equals("")) {
			return CLibraryWrapper.INSTANCE.chdir(System.getenv("HOME"));
		}
		return CLibraryWrapper.INSTANCE.chdir(path);
	}

	public static String getWorkingDirectory() {
		int size = 256;
		byte[] buffers = new byte[size];
		CLibraryWrapper.INSTANCE.getcwd(buffers, size);
		int actualSize = 0;
		for(byte buf : buffers) {
			if(buf == 0) {
				break;
			}
			actualSize++;
		}
		byte[] newBuffers = new byte[actualSize];
		for(int i = 0; i < actualSize; i++) {
			newBuffers[i] = buffers[i];
		}
		return new String(newBuffers);
	}
}

abstract class BuiltinCommand extends PseudoProcess {
	public void setProcPosition(ProcPosition procPosition) {
	}	// do nothing

	public void setMergeType(MergeType mergeType) {
	}	// do nothing

	public void setInputRedirect(String readFileName) {
	}	// do nothing

	public void setOutputRedirect(OutputType fd, String writeFileName, boolean append) {
	}	// do nothing

	abstract public void start();

	public void kill() { // do nothing
	}

	public void waitTermination() { // do nothing
	}
}

class Command_cd extends BuiltinCommand {
	@Override
	public void start() {
		int size = this.commandList.size();
		String path = "";
		if(size > 1) {
			path = this.commandList.get(1);
		}
		this.retValue = BuiltinCommandMap.changeDirectory(path);
	}
}

class Command_exit extends BuiltinCommand {
	@Override
	public void start() {
		int status = 0;
		int size = this.commandList.size();
		if(size > 1) {
			try {
				status = Integer.parseInt(this.commandList.get(1));
			}
			catch(NumberFormatException e) {
			}
		}
		System.exit(status);
	}
}