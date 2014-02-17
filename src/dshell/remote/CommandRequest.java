package dshell.remote;

import static dshell.lib.TaskOption.Behavior.background;
import static dshell.lib.TaskOption.Behavior.printable;
import static dshell.lib.TaskOption.Behavior.receivable;
import static dshell.lib.TaskOption.Behavior.returnable;
import static dshell.lib.TaskOption.Behavior.throwable;
import static dshell.lib.TaskOption.RetType.TaskType;
import static dshell.lib.TaskOption.RetType.VoidType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base64;

import dshell.lib.RuntimeContext;
import dshell.lib.TaskOption;
import dshell.util.Utils;

public class CommandRequest implements Serializable {
	private static final long serialVersionUID = 2907070450718884160L;
	private final RuntimeContext context;
	private final ArrayList<ArrayList<String>> cmdsList;
	private final TaskOption option;

	public CommandRequest(ArrayList<ArrayList<String>> cmdsList, boolean isBackground) {
		this.context = RuntimeContext.getContext();
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

	public RuntimeContext getContext() {
		return this.context;
	}

	public static String encodeToString(CommandRequest request) {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(buffer);
			oos.writeObject(request);
			oos.close();
			return Base64.encodeBase64String(buffer.toByteArray());
		}
		catch (IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
		return null;
	}

	public static CommandRequest decodeFromString(String code) {
		ByteArrayInputStream buffer = new ByteArrayInputStream(Base64.decodeBase64(code));
		try {
			ObjectInputStream ois = new ObjectInputStream(buffer);
			CommandRequest request = (CommandRequest) ois.readObject();
			ois.close();
			return request;
		}
		catch (IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			Utils.fatal(1, "invalid class");
		}
		return null;
	}
}
