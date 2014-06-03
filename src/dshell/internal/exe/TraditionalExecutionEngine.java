package dshell.internal.exe;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.EmptyNode;
import libbun.ast.decl.BunClassNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.ast.decl.TopLevelNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.statement.BunReturnNode;
import libbun.parser.classic.BTokenContext;
import libbun.parser.classic.ParserSource;
import libbun.type.BType;

import dshell.internal.ast.DShellWrapperNode;
import dshell.internal.ast.InternalFuncCallNode;
import dshell.internal.ast.sugar.DShellExportEnvNode;
import dshell.internal.ast.sugar.DShellImportEnvNode;
import dshell.internal.jvm.JavaByteCodeGenerator;
import dshell.internal.jvm.JavaStaticFieldNode;
import dshell.internal.lang.DShellTypeChecker;
import dshell.internal.lib.RuntimeContext;
import dshell.internal.lib.StreamUtils;
import dshell.internal.lib.Utils;

public class TraditionalExecutionEngine implements ExecutionEngine {
	protected final JavaByteCodeGenerator generator;
	protected BunFunctionNode untypedMainNode = null;
	protected TopLevelStatementInfo stmtInfo = null;
	protected boolean hasVarInitialized = false;

	protected TraditionalExecutionEngine(JavaByteCodeGenerator generator) {
		this.generator = generator;
	}

	protected ArrayList<BNode> parseScript(String script, String fileName, int lineNum) {
		ArrayList<BNode> nodeList = new ArrayList<BNode>();
		BunBlockNode topBlockNode = new BunBlockNode(null, this.generator.RootGamma);
		ParserSource source = new ParserSource(fileName, lineNum, script, this.generator.Logger);
		BTokenContext tokenContext = new BTokenContext(this.generator.RootParser, this.generator, source, 0, script.length());
		tokenContext.SkipEmptyStatement();
		while(tokenContext.HasNext()) {
			tokenContext.SetParseFlag(BTokenContext._NotAllowSkipIndent);
			topBlockNode.ClearListToSize(0);
			try {
				BNode stmtNode = tokenContext.ParsePattern(topBlockNode, "$Statement$", BTokenContext._Required);
				nodeList.add(stmtNode);
			}
			catch(Throwable e) {
				System.err.println("Parsing Problem");
				e.printStackTrace();
				return null;
			}
			tokenContext.SkipEmptyStatement();
		}
		this.generator.Logger.OutputErrorsToStdErr();
		return nodeList;
	}

	@Override
	public void setArg(String[] scriptArgs) {
		StringBuilder argvBuilder = new StringBuilder();
		argvBuilder.append("let ARGV = [");
		for(int i = 0; i < scriptArgs.length; i++) {
			if(i != 0) {
				argvBuilder.append(", ");
			}
			argvBuilder.append("\"");
			argvBuilder.append(scriptArgs[i]);
			argvBuilder.append("\"");
		}
		argvBuilder.append("]");
		// parse
		ArrayList<BNode> nodeList = this.parseScript(argvBuilder.toString(), "(arg)", 0);
		if(nodeList == null) {
			System.exit(1);
		}
		// generate top level wrapper
		for(BNode node : nodeList) {
			if(!this.generateStatement(node, false)) {
				System.exit(1);
			}
		}
	}

	@Override
	public void eval(String scriptName) {
		String source = this.readFromFile(scriptName);
		if(source == null) {
			System.err.println("file not found: " + scriptName);
			System.exit(1);
		}
		this.eval(scriptName, source);
	}

	@Override
	public void eval(String scriptName, String source) {
		this.loadGlobalVariable(false);
		// parse script
		ArrayList<BNode> nodeList = this.parseScript(source, scriptName, 1);
		if(nodeList == null) {
			System.exit(1);
		}
		// generate top level wrapper
		for(BNode node : nodeList) {
			if(!this.generateStatement(node, false)) {
				System.err.println("abort loading: " + scriptName);
				System.exit(1);
			}
		}
		// invoke
		this.invokeMain();
	}

	protected void invokeMain() {
		if(this.untypedMainNode == null) {
			System.err.println("not found main");
			System.exit(1);
		}
		try {
			BunFunctionNode node = (BunFunctionNode) this.generator.TypeChecker.CheckType(this.untypedMainNode, BType.VarType);
			node.Type = BType.VoidType;
			node.IsExport = true;
			node.Accept(this.generator);
			this.generator.Logger.OutputErrorsToStdErr();
		}
		catch(JavaByteCodeGenerator.ErrorNodeFoundException e) {
			this.generator.Logger.OutputErrorsToStdErr();
			if(RuntimeContext.getContext().isDebugMode()) {
				e.printStackTrace();
			}
			System.exit(1);
		}
		catch(Throwable e) {
			e.printStackTrace();
			System.err.println("Code Generation Failed");
			System.exit(1);
		}
		try {
			JavaStaticFieldNode mainFunc = this.generator.getMainFuncNode();
			if(mainFunc == null) {
				System.err.println("Code Generation Failed");
				System.exit(1);
			}
			Method Method = mainFunc.getStaticClass().getMethod("f");
			Method.invoke(null);
			System.exit(0);
		}
		catch(InvocationTargetException e) {
			Utils.printException(e);
			System.exit(1);
		}
		catch(Throwable e) {
			e.printStackTrace();
			Utils.fatal(1, "invocation problem");
		}
	}

	@Override
	public void eval(String source, int lineNum) {
		this.eval("(stdin)", source, lineNum);
	}

	protected void eval(String sourceName, String source, int lineNum) {
		this.loadGlobalVariable(true);
		// parse script
		ArrayList<BNode> nodeList = this.parseScript(source, sourceName, lineNum);
		if(nodeList == null) {
			return;
		}
		for(BNode node : nodeList) {
			if(!this.generateStatement(node, true) || !this.invokeStatement()) {
				this.generator.Logger.OutputErrorsToStdErr();
				return;
			}
		}
	}

	protected void loadGlobalVariable(boolean isInteractive) {
		if(this.hasVarInitialized) {
			return;
		}
		this.hasVarInitialized = true;
		BNode parentNode = new BunBlockNode(null, this.generator.RootGamma);
		ArrayList<BNode> nodeList = new ArrayList<BNode>();
		nodeList.add(this.createVarNode(parentNode, "stdin", StreamUtils.class, "createStdin"));
		nodeList.add(this.createVarNode(parentNode, "stdout", StreamUtils.class, "createStdout"));
		nodeList.add(this.createVarNode(parentNode, "stderr", StreamUtils.class, "createStderr"));
		for(BNode node : nodeList) {
			if(!this.generateStatement(node, isInteractive)) {
				System.exit(1);
			}
			if(isInteractive) {
				this.invokeStatement();
			}
		}
	}

	protected BNode createVarNode(BNode parentNode, String varName, Class<?> holderClass, String methodName) {
		BunLetVarNode node = new BunLetVarNode(parentNode, BunLetVarNode._IsReadOnly, null, varName);
		node.SetNode(BunLetVarNode._InitValue, new InternalFuncCallNode(this.generator.getTypeTable(), node, holderClass, methodName));
		return node;
	}

	@Override
	public void loadDShellRC() {
		final String fileName = RuntimeContext.getContext().getenv("HOME") + "/.dshellrc";
		String source = this.readFromFile(fileName);
		if(source != null) {
			this.eval(fileName, source, 0);
		}
	}

	protected boolean invokeStatement() {
		Class<?> funcClass = this.generator.getDefinedFunctionClass(this.stmtInfo.funcName, BType.VoidType, 0);
		try {
			Method method = funcClass.getMethod("f");
			Object value = method.invoke(null);
			if(!this.stmtInfo.returnType.IsVoidType()) {
				System.out.println(" (" + this.stmtInfo.returnType + ") " + value);
			}
			return true;
		}
		catch(InvocationTargetException e) {
			Utils.printException(e);
		}
		catch(Throwable e) {
			e.printStackTrace();
			Utils.fatal(1, "invocation problem");
		}
		return false;
	}

	protected boolean generateStatement(BNode node, boolean isInteractive) {
		if(node instanceof EmptyNode) {
			return true;
		}
		if(node instanceof TopLevelNode) {
			((TopLevelNode)node).Perform(this.generator.RootGamma);
			return true;
		}
		node = this.checkTopLevelSupport(node);
		if(node.IsErrorNode()) {
			return this.generateByteCode(node);
		}
		else if(isInteractive && (node instanceof DShellWrapperNode) && !((DShellWrapperNode)node).isVarTarget()) {
			node = this.generator.TypeChecker.CheckType(node, BType.VoidType);
			return this.generateByteCode(node);
		}
		else if(isInteractive) {
			BunFunctionNode funcNode = ((DShellTypeChecker)this.generator.TypeChecker).visitTopLevelStatementNode(node);
			this.stmtInfo = new TopLevelStatementInfo(funcNode.GivenName, funcNode.ReturnType());
			return this.generateByteCode(funcNode);
		}
		else {
			if(this.untypedMainNode == null) {
				this.untypedMainNode = new BunFunctionNode(node.ParentNode);
				this.untypedMainNode.GivenName = "main";
				this.untypedMainNode.SourceToken = node.SourceToken;
				this.untypedMainNode.SetNode(BunFunctionNode._Block, new BunBlockNode(this.untypedMainNode, null));
			}
			this.untypedMainNode.BlockNode().Append(node);
		}
		return true;
	}

	private boolean generateByteCode(BNode node) {
		try {
			node.Accept(this.generator);
			return true;
		}
		catch(JavaByteCodeGenerator.ErrorNodeFoundException e) {
			if(RuntimeContext.getContext().isDebugMode()) {
				e.printStackTrace();
			}
		}
		catch(Throwable e) {
			System.err.println("Code Generation Failed");
			e.printStackTrace();
		}
		return false;
	}

	private BNode checkTopLevelSupport(BNode Node) {
		if(Node instanceof BunVarBlockNode) {
			return new ErrorNode(Node, "only available inside function");
		}
		else if(Node instanceof BunClassNode || Node instanceof BunFunctionNode) {
			return new DShellWrapperNode(Node);
		}
		else if(Node instanceof BunLetVarNode || Node instanceof DShellExportEnvNode || Node instanceof DShellImportEnvNode) {
			return new DShellWrapperNode(Node, true);
		}
		BunReturnNode ReturnNode = this.findReturnNode(Node);
		if(ReturnNode != null) {
			return new ErrorNode(ReturnNode, "only available inside function");
		}
		return Node;
	}

	private BunReturnNode findReturnNode(BNode Node) {
		if(Node == null || Node.IsErrorNode()) {
			return null;
		}
		else if(Node instanceof BunReturnNode) {
			return (BunReturnNode) Node;
		}
		else if(Node instanceof BunBlockNode) {
			BunBlockNode BlockNode = (BunBlockNode) Node;
			int size = BlockNode.GetListSize();
			for(int i = 0; i < size; i++) {
				BunReturnNode ReturnNode = this.findReturnNode(BlockNode.GetListAt(i));
				if(ReturnNode != null) {
					return ReturnNode;
				}
			}
		}
		int size = Node.GetAstSize();
		for(int i = 0; i < size; i++) {
			BunReturnNode ReturnNode = this.findReturnNode(Node.AST[i]);
			if(ReturnNode != null) {
				return ReturnNode;
			}
		}
		return null;
	}

	protected String readFromFile(String fileName) {
		FileInputStream fileInput;
		try {
			fileInput = new FileInputStream(fileName);
		}
		catch(FileNotFoundException e) {
			return null;
		}
		ByteArrayOutputStream streamBuffer = new ByteArrayOutputStream();
		int bufferSize = 2048;
		int read = 0;
		byte[] buffer = new byte[bufferSize];
		try(BufferedInputStream stream = new BufferedInputStream(fileInput)) {
			while((read = stream.read(buffer, 0, bufferSize)) > -1) {
				streamBuffer.write(buffer, 0, read);
			}
			return streamBuffer.toString();
		}
		catch(IOException e) {
			e.printStackTrace();
			Utils.fatal(1, "IO problem");
		}
		return null;
	}

	protected static class TopLevelStatementInfo {
		public final String funcName;
		public final BType returnType;

		public TopLevelStatementInfo(String funcName, BType returnType) {
			this.funcName = funcName;
			this.returnType = returnType;
		}
	}
}

