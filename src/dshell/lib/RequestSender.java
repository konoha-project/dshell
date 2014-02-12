package dshell.lib;

import java.io.Serializable;
import java.util.ArrayList;

import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.Behavior.printable ;
import static dshell.lib.TaskOption.Behavior.throwable ;
import static dshell.lib.TaskOption.Behavior.background;
import static dshell.lib.TaskOption.Behavior.receivable;

import static dshell.lib.TaskOption.RetType.VoidType   ;
import static dshell.lib.TaskOption.RetType.TaskType   ;

public class RequestSender extends PseudoProcess {
	private final CommandRequest request;
	private final boolean isBackground;

	public RequestSender(ArrayList<ArrayList<String>> cmdsList, boolean isBackground) {
		this.request = new CommandRequest(cmdsList, isBackground);
		this.isBackground = isBackground;
	}

	@Override
	public void mergeErrorToOut() {
	}
	@Override
	public void setInputRedirect(String readFileName) {
	}
	@Override
	public void setOutputRedirect(int fd, String writeFileName, boolean append) {
	}

	@Override
	public void start() {

	}

	@Override
	public void kill() {
	}

	@Override
	public void waitTermination() {

	}

	@Override
	public boolean checkTermination() {

		return false;
	}
}

class CommandRequest implements Serializable {
	private static final long serialVersionUID = 2907070450718884160L;
	private final ArrayList<ArrayList<String>> cmdsList;
	private final TaskOption option;

	public CommandRequest(ArrayList<ArrayList<String>> cmdsList, boolean isBackground) {
		this.cmdsList = cmdsList;
		if(isBackground) {
			this.option = TaskOption.of(VoidType, background, printable, receivable);
		}
		else {
			this.option = TaskOption.of(TaskType, returnable, throwable, receivable);
		}
		
	}

	public ArrayList<ArrayList<String>> getCmdsList() {
		return this.cmdsList;
	}

	public TaskOption getOption() {
		return this.option;
	}
}
