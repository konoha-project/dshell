package dshell.internal.remote;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import static dshell.internal.process.TaskOption.Behavior.background;
import dshell.internal.lib.RuntimeContext;
import dshell.internal.lib.Utils;
import dshell.internal.process.CommandArg;
import dshell.internal.process.TaskBuilder;
import dshell.internal.process.TaskOption;
import dshell.lang.Task;

public class RequestReceiver {
	public static void invoke(String requestString) {
		try {
			CommandRequest request = CommandRequest.decodeFromString(requestString);
			RuntimeContext.loadContext(request.getContext());
			ArrayList<ArrayList<CommandArg>> cmdsList = request.getCmdsList();
			TaskOption option = request.getOption();
			if(option.is(background)) {
				TaskBuilder.createTask(cmdsList, option);
			}
			else {
				Task task = (Task) TaskBuilder.createTask(cmdsList, option);
				new ObjectOutputStream(System.out).writeObject(task);
			}
			System.exit(0);
		}
		catch (IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
	}
}
