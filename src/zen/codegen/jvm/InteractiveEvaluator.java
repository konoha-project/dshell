package zen.codegen.jvm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import dshell.ast.DShellCatchNode;
import dshell.ast.DShellCommandNode;
import dshell.ast.DShellDummyNode;
import dshell.ast.DShellTryNode;
import dshell.lang.DShellVisitor;
import dshell.lib.Task;
import dshell.lib.TaskBuilder;
import dshell.remote.TaskArray;
import zen.ast.ZAndNode;
import zen.ast.ZArrayLiteralNode;
import zen.ast.ZAsmNode;
import zen.ast.ZBinaryNode;
import zen.ast.ZBlockNode;
import zen.ast.ZBooleanNode;
import zen.ast.ZBreakNode;
import zen.ast.ZCastNode;
import zen.ast.ZClassNode;
import zen.ast.ZComparatorNode;
import zen.ast.ZErrorNode;
import zen.ast.ZFloatNode;
import zen.ast.ZFuncCallNode;
import zen.ast.ZFunctionNode;
import zen.ast.ZGetIndexNode;
import zen.ast.ZGetNameNode;
import zen.ast.ZGetterNode;
import zen.ast.ZGlobalNameNode;
import zen.ast.ZGroupNode;
import zen.ast.ZIfNode;
import zen.ast.ZInstanceOfNode;
import zen.ast.ZIntNode;
import zen.ast.ZLetNode;
import zen.ast.ZListNode;
import zen.ast.ZMacroNode;
import zen.ast.ZMapEntryNode;
import zen.ast.ZMapLiteralNode;
import zen.ast.ZMethodCallNode;
import zen.ast.ZNewObjectNode;
import zen.ast.ZNode;
import zen.ast.ZNotNode;
import zen.ast.ZNullNode;
import zen.ast.ZOrNode;
import zen.ast.ZReturnNode;
import zen.ast.ZSetIndexNode;
import zen.ast.ZSetNameNode;
import zen.ast.ZSetterNode;
import zen.ast.ZStringNode;
import zen.ast.ZThrowNode;
import zen.ast.ZTryNode;
import zen.ast.ZUnaryNode;
import zen.ast.ZVarNode;
import zen.ast.ZWhileNode;
import zen.ast.sugar.ZLocalDefinedNode;
import zen.ast.sugar.ZTopLevelNode;
import zen.parser.ZEmptyValue;
import zen.parser.ZGenerator;
import zen.parser.ZLogger;
import zen.parser.ZMacroFunc;
import zen.type.ZFunc;
import zen.type.ZFuncType;
import zen.util.LibZen;
import zen.util.ZFloatArray;
import zen.util.ZIntArray;
import zen.util.ZObjectArray;
import zen.util.ZStringArray;
import zen.util.ZenMap;

public class InteractiveEvaluator extends ZGenerator implements DShellVisitor {
	private final ModifiedAsmGenerator generator;
	private Object EvaledValue;
	protected InteractiveEvaluator(ModifiedAsmGenerator generator) {
		super(generator.LangInfo);
		this.generator = generator;
	}

	public Object getEvaledValue() {
		return this.EvaledValue;
	}

	public final Object Eval(ZNode Node) {
		if(this.IsVisitable()) {
			Node.Accept(this);
		}
		return this.EvaledValue;
	}

	protected ZNode[] PackNodes(ZNode Node, ZListNode List) {
		int Start = 0;
		ZNode[] Nodes = new ZNode[List.GetListSize() + Start];
		if(Node != null) {
			Start = 1;
			Nodes[0] = Node;
		}
		for(int i = 0; i < Nodes.length; i++) {
			Nodes[i+Start] = List.GetListAt(i);
		}
		return Nodes;
	}

	Object NumberCast(Class<?> T, Number Value) {
		if(T == int.class || T == Integer.class) {
			return Value.intValue();
		}
		if(T == long.class || T == Long.class) {
			return Value.longValue();
		}
		if(T == double.class || T == Double.class) {
			return Value.longValue();
		}
		if(T == short.class || T == Short.class) {
			return Value.shortValue();
		}
		if(T == float.class || T == Float.class) {
			return Value.floatValue();
		}
		if(T == byte.class || T == Byte.class) {
			return Value.byteValue();
		}
		return Value;
	}

	void EvalConstructor(ZNode Node, Constructor<?> jMethod, ZListNode ListNode) {
		try {
			Object Values[] = new Object[ListNode.GetListSize()];
			Class<?> P[] = jMethod.getParameterTypes();
			for(int i = 0; i < ListNode.GetListSize(); i++) {
				Values[i] = this.Eval(ListNode.GetListAt(i));
				if(Values[i] instanceof Number) {
					Values[i] = this.NumberCast(P[i], (Number)Values[i]);
				}
			}
			if(this.IsVisitable()) {
				this.EvaledValue = jMethod.newInstance(Values);
			}
		} catch (Exception e) {
			ZLogger._LogError(Node.SourceToken, "runtime error: " + e);
			e.printStackTrace();
			this.StopVisitor();
		}
	}

	void EvalMethod(ZNode Node, Method jMethod, ZNode RecvNode, ZNode[] Nodes) {
		try {
			Object Recv = null;
			if(RecvNode != null && !Modifier.isStatic(jMethod.getModifiers())) {
				Recv = this.Eval(RecvNode);
			}
			Object Values[] = new Object[Nodes.length];
			Class<?> P[] = jMethod.getParameterTypes();
			for(int i = 0; i < Nodes.length; i++) {
				Values[i] = this.Eval(Nodes[i]);
				if(Values[i] instanceof Number) {
					Values[i] = this.NumberCast(P[i], (Number)Values[i]);
				}
			}
			if(this.IsVisitable()) {
				this.EvaledValue = jMethod.invoke(Recv, Values);
				if(jMethod.getReturnType() == void.class) {
					this.EvaledValue = ZEmptyValue._TrueEmpty;
				}
			}
		}
		catch(java.lang.reflect.InvocationTargetException e) {
			Throwable te = e.getCause();
			ZLogger._LogError(Node.SourceToken, "runtime error: " + te);
			te.printStackTrace();
			this.StopVisitor();
		}
		catch (Exception e) {
			ZLogger._LogInfo(Node.SourceToken, "runtime error: " + e);
			e.printStackTrace();
			this.StopVisitor();
		}
	}

	void EvalStaticMethod(ZNode Node, Method sMethod, ZNode[] Nodes) {
		this.EvalMethod(Node, sMethod, null, Nodes);
	}

	Method GetInvokeMethod(Object Recv) {
		Method[] m = Recv.getClass().getMethods();
		for(int i=0; i < m.length;i++) {
			if(m[i].getName().equals("Invoke")) {
				return m[i];
			}
		}
		return null;
	}

	void InvokeMethod(ZNode Node, Object Recv, ZListNode ListNode) {
		try {
			Method jMethod = this.GetInvokeMethod(Recv);
			//System.out.println("Recv="+Recv.getClass() + ", jMethod="+jMethod + "params=" + ListNode.GetListSize());
			Object Values[] = new Object[ListNode.GetListSize()];
			Class<?> P[] = jMethod.getParameterTypes();
			for(int i = 0; i < ListNode.GetListSize(); i++) {
				Values[i] = this.Eval(ListNode.GetListAt(i));
				if(Values[i] instanceof Number) {
					Values[i] = this.NumberCast(P[i], (Number)Values[i]);
				}
			}
			if(this.IsVisitable()) {
				this.EvaledValue = jMethod.invoke(Recv, Values);
				if(jMethod.getReturnType() == void.class) {
					this.EvaledValue = ZEmptyValue._TrueEmpty;
				}
			}
		} catch(java.lang.reflect.InvocationTargetException e) {
			Throwable te = e.getCause();
			ZLogger._LogError(Node.SourceToken, "invocation error: " + te);
			this.StopVisitor();
		} catch (Exception e) {
			ZLogger._LogError(Node.SourceToken, "invocation error: " + e);
			this.StopVisitor();
		}
	}

	@Override
	public void VisitCommandNode(DShellCommandNode Node) {
		ArrayList<DShellCommandNode> nodeList = new ArrayList<DShellCommandNode>();
		DShellCommandNode node = Node;
		while(node != null) {
			nodeList.add(node);
			node = (DShellCommandNode) node.PipedNextNode;
		}
		int size = nodeList.size();
		String[][] values = new String[size][];
		for(int i = 0; i < size; i++) {
			DShellCommandNode currentNode = nodeList.get(i);
			int listSize = currentNode.GetListSize();
			values[i] = new String[listSize];
			for(int j = 0; j < listSize; j++) {
				values[i][j] = (String)this.Eval(currentNode.GetListAt(j));
			}
		}
		try {
			if(Node.Type.IsBooleanType()) {
				this.EvaledValue = TaskBuilder.ExecCommandBool(values);
			}
			else if(Node.Type.IsIntType()) {
				this.EvaledValue = TaskBuilder.ExecCommandInt(values);
			}
			else if(Node.Type.IsStringType()) {
				this.EvaledValue = TaskBuilder.ExecCommandString(values);
			}
			else if(this.generator.GetJavaClass(Node.Type).equals(Task.class)) {
				this.EvaledValue = TaskBuilder.ExecCommandTask(values);
			}
			else if(this.generator.GetJavaClass(Node.Type).equals(TaskArray.class)) {
				this.EvaledValue = TaskBuilder.ExecCommandTaskArray(values);
			}
			else {
				TaskBuilder.ExecCommandVoid(values);
			}
		}
		catch(Exception e) {
			ZLogger._LogError(Node.SourceToken, "invocation error: " + e);
			this.StopVisitor();
		}
	}

	@Override
	public void VisitTryNode(DShellTryNode Node) {
		this.VisitUndefinedNode(Node);	// FIXME
	}

	@Override
	public void VisitCatchNode(DShellCatchNode Node) {
		this.VisitUndefinedNode(Node);	//FIXME
	}

	@Override public void VisitInstanceOfNode(ZInstanceOfNode Node) {
		Class<?> JavaClass = this.generator.GetJavaClass(Node.TargetType());
		if(Node.TargetType().IsIntType()) {
			JavaClass = Long.class;
		}
		else if(Node.TargetType().IsFloatType()) {
			JavaClass = Double.class;
		}
		else if(Node.TargetType().IsBooleanType()) {
			JavaClass = Boolean.class;
		}

		ZNode TargetNode = Node.LeftNode();
		Object Value = this.Eval(TargetNode);
		if(TargetNode.Type.IsIntType()) {
			Value = new Long((Long) Value);
		}
		else if(TargetNode.Type.IsFloatType()) {
			Value = new Double((Double) Value);
		}
		else if(TargetNode.Type.IsBooleanType()) {
			Value = new Boolean((Boolean) Value);
		}
		this.EvaledValue = Value.getClass().equals(JavaClass);
	}

	@Override
	public void VisitDummyNode(DShellDummyNode Node) {	// do nothing
	}

	@Override
	public void VisitNullNode(ZNullNode Node) {
		this.EvaledValue = null;
	}

	@Override
	public void VisitBooleanNode(ZBooleanNode Node) {
		this.EvaledValue = Node.BooleanValue;
	}

	@Override
	public void VisitIntNode(ZIntNode Node) {
		this.EvaledValue = Node.IntValue;
	}

	@Override
	public void VisitFloatNode(ZFloatNode Node) {
		this.EvaledValue = Node.FloatValue;
	}

	@Override
	public void VisitStringNode(ZStringNode Node) {
		this.EvaledValue = Node.StringValue;
	}

	@Override
	public void VisitArrayLiteralNode(ZArrayLiteralNode Node) {
		if(Node.IsUntyped()) {
			ZLogger._LogError(Node.SourceToken, "ambigious array");
			this.StopVisitor();
		}
		else if(Node.Type.GetParamType(0).IsIntType()) {
			long Values[] = new long[Node.GetListSize()];
			for(int i = 0; i < Node.GetListSize(); i++) {
				Object Value = this.Eval(Node.GetListAt(i));
				if(Value instanceof Number) {
					Values[i] = ((Number)Value).longValue();
				}
			}
			if(this.IsVisitable()) {
				this.EvaledValue = new ZIntArray(Node.Type.TypeId, Values);
			}
		}
		else if(Node.Type.GetParamType(0).IsFloatType()) {
			double Values[] = new double[Node.GetListSize()];
			for(int i = 0; i < Node.GetListSize(); i++) {
				Object Value = this.Eval(Node.GetListAt(i));
				if(Value instanceof Number) {
					Values[i] = ((Number)Value).doubleValue();
				}
			}
			if(this.IsVisitable()) {
				this.EvaledValue = new ZFloatArray(Node.Type.TypeId, Values);
			}
		}
		else if(Node.Type.GetParamType(0).IsIntType()) {
			String Values[] = new String[Node.GetListSize()];
			for(int i = 0; i < Node.GetListSize(); i++) {
				Object Value = this.Eval(Node.GetListAt(i));
				if(Value instanceof String) {
					Values[i] = (String)Value;
				}
			}
			if(this.IsVisitable()) {
				this.EvaledValue = new ZStringArray(Node.Type.TypeId, Values);
			}
		}
		else {
			Object Values[] = new Object[Node.GetListSize()];
			for(int i = 0; i < Node.GetListSize(); i++) {
				Values[i] = this.Eval(Node.GetListAt(i));
			}
			if(this.IsVisitable()) {
				this.EvaledValue = new ZObjectArray(Node.Type.TypeId, Values);
			}
		}
	}

	@Override
	public void VisitMapLiteralNode(ZMapLiteralNode Node) {
		if(Node.IsUntyped()) {
			ZLogger._LogError(Node.SourceToken, "ambigious map");
			this.StopVisitor();
		}
		else {
			Object Values[] = new Object[Node.GetListSize()*2];
			for(int i = 0; i < Node.GetListSize(); i = i + 2) {
				ZMapEntryNode EntryNode = Node.GetMapEntryNode(i);
				Values[i*2] = EntryNode.Name;
				Values[i*2+1] = this.Eval(EntryNode.ValueNode());
			}
			if(this.IsVisitable()) {
				this.EvaledValue = new ZenMap<Object>(Node.Type.TypeId, Values);
			}
		}
	}

	@Override
	public void VisitNewObjectNode(ZNewObjectNode Node) {
		Constructor<?> jMethod = this.generator.GetConstructor(Node.Type, Node);
		if(jMethod != null) {
			this.EvalConstructor(Node, jMethod, Node);
		}
		else {
			ZLogger._LogError(Node.SourceToken, "no constructor: " + Node.Type);
			this.StopVisitor();
		}
	}

	@Override
	public void VisitGlobalNameNode(ZGlobalNameNode Node) {
		//TODO
		
	}

	@Override
	public void VisitGetNameNode(ZGetNameNode Node) {
		ZNode Node1 = Node.GetNameSpace().GetSymbolNode(Node.VarName);
		if(Node1 != null) {
			this.EvaledValue = this.Eval(Node1);
		}
		else {
			ZLogger._LogError(Node.SourceToken, "undefined symbol: " + Node.VarName);
			this.StopVisitor();
		}
	}

	@Override
	public void VisitSetNameNode(ZSetNameNode Node) {
		//TODO
		
	}

	@Override
	public void VisitGroupNode(ZGroupNode Node) {
		this.EvaledValue = this.Eval(Node.ExprNode());
	}

	@Override
	public void VisitGetterNode(ZGetterNode Node) {
		Method sMethod = JavaMethodTable.GetStaticMethod("GetField");
		ZNode NameNode = new ZStringNode(Node, null, Node.GetName());
		this.EvalStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode(), NameNode});
	}

	@Override
	public void VisitSetterNode(ZSetterNode Node) {
		Method sMethod = JavaMethodTable.GetStaticMethod("SetField");
		ZNode NameNode = new ZStringNode(Node, null, Node.GetName());
		this.EvalStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode(), NameNode, Node.ExprNode()});
	}

	@Override
	public void VisitGetIndexNode(ZGetIndexNode Node) {
		Method sMethod = JavaMethodTable.GetBinaryStaticMethod(Node.GetAstType(ZGetIndexNode._Recv), "[]", Node.GetAstType(ZGetIndexNode._Index));
		if(sMethod == null) {
			ZLogger._LogError(Node.SourceToken, "type error");
			return ;
		}
		this.EvalStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode(), Node.IndexNode()});
	}

	@Override
	public void VisitSetIndexNode(ZSetIndexNode Node) {
		Method sMethod = JavaMethodTable.GetBinaryStaticMethod(Node.GetAstType(ZSetIndexNode._Recv), "[]", Node.GetAstType(ZSetIndexNode._Index));
		if(sMethod == null) {
			ZLogger._LogError(Node.SourceToken, "type error");
			return ;
		}
		this.EvalStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode(), Node.IndexNode(), Node.ExprNode()});
	}

	@Override
	public void VisitMethodCallNode(ZMethodCallNode Node) {
		Method jMethod = this.generator.GetMethod(Node.RecvNode().Type, Node.MethodName(), Node);
		if(jMethod != null) {
			this.EvalMethod(Node, jMethod, Node.RecvNode(), this.PackNodes(null, Node));
		}
		else {
			ZLogger._LogError(Node.SourceToken, "no method: " + Node.MethodName() + " of " + Node.RecvNode().Type);
			this.StopVisitor();
		}
	}

	@Override
	public void VisitFuncCallNode(ZFuncCallNode Node) {
		ZFuncType FuncType = Node.GetFuncType();
		if(FuncType == null) {
			ZLogger._LogError(Node.SourceToken, "not function");
			this.StopVisitor();
		}
		else {
			String FuncName = Node.GetStaticFuncName();
			if(FuncName != null) {
				this.generator.LazyBuild(FuncType.StringfySignature(FuncName));
				Class<?> FunctionClass = this.generator.GetDefinedFunctionClass(FuncName, FuncType);
				Node.SetNode(ZFuncCallNode._Func, new JavaStaticFieldNode(Node, FunctionClass, FuncType, "function"));
			}
		}
		Object Recv = this.Eval(Node.FuncNameNode());
		if(this.IsVisitable()) {
			this.InvokeMethod(Node, Recv, Node);
		}
	}

	private Method LookupStaticMethod(ZMacroFunc MacroFunc) {
		String MacroText = MacroFunc.MacroText;
		int ClassEnd = MacroText.indexOf(".");
		int MethodEnd = MacroText.indexOf("(");
		//System.out.println("MacroText: " + MacroText + " " + ClassEnd + ", " + MethodEnd);
		String ClassName = MacroText.substring(0, ClassEnd);
		ClassName = ClassName.replaceAll("/", ".");
		String MethodName = MacroText.substring(ClassEnd+1, MethodEnd);
		try {
			Class<?> C= Class.forName(ClassName);
			Class<?>[] P = new Class<?>[MacroFunc.GetFuncType().GetFuncParamSize()];
			for(int i = 0; i < P.length; i++) {
				P[i] = this.generator.GetJavaClass(MacroFunc.GetFuncType().GetFuncParamType(i));
			}
			Method M = C.getMethod(MethodName, P);
			return M;
		}
		catch(Exception e) {
			System.out.println(e);
		}
		return null;
	}

	@Override
	public void VisitMacroNode(ZMacroNode FuncNode) {
		this.EvalStaticMethod(FuncNode, this.LookupStaticMethod(FuncNode.MacroFunc), this.PackNodes(null, FuncNode));
	}

	@Override
	public void VisitUnaryNode(ZUnaryNode Node) {
		Method sMethod = JavaMethodTable.GetUnaryStaticMethod(Node.SourceToken.GetText(), Node.RecvNode().Type);
		this.EvalStaticMethod(Node, sMethod, new ZNode[] {Node.RecvNode()});
	}

	@Override
	public void VisitNotNode(ZNotNode Node) {
		Method sMethod = JavaMethodTable.GetUnaryStaticMethod(Node.SourceToken.GetText(), Node.AST[ZNotNode._Recv].Type);
		this.EvalStaticMethod(Node, sMethod, new ZNode[] {Node.AST[ZNotNode._Recv]});
	}

	@Override
	public void VisitCastNode(ZCastNode Node) {
		if(Node.Type.IsVoidType()) {
			this.EvaledValue = this.Eval(Node.ExprNode());
		}
		else {
			ZFunc Func = this.generator.LookupConverterFunc(Node.ExprNode().Type, Node.Type);
			if(Func instanceof ZMacroFunc) {
				this.EvalStaticMethod(Node, this.LookupStaticMethod((ZMacroFunc)Func), new ZNode[] {Node.ExprNode()});
				return;
			}
			this.EvaledValue = this.Eval(Node.ExprNode());
			if(this.EvaledValue == null) {
				return;
			}
			if(this.IsVisitable()) {
				Class<?> CastClass = this.generator.GetJavaClass(Node.Type);
				if(CastClass.isAssignableFrom(this.EvaledValue.getClass())) {
					return ;
				}
				else {
					ZLogger._LogError(Node.SourceToken, "no type coercion: " + Node.ExprNode().Type + " to " + Node.Type);
					this.StopVisitor();
				}
			}
		}
	}

	@Override
	public void VisitBinaryNode(ZBinaryNode Node) {
		Method sMethod = JavaMethodTable.GetBinaryStaticMethod(Node.LeftNode().Type, Node.SourceToken.GetText(), Node.RightNode().Type);
		this.EvalStaticMethod(Node, sMethod, new ZNode[] {Node.LeftNode(), Node.RightNode()});
	}

	@Override
	public void VisitComparatorNode(ZComparatorNode Node) {
		Method sMethod = JavaMethodTable.GetBinaryStaticMethod(Node.LeftNode().Type, Node.SourceToken.GetText(), Node.RightNode().Type);
		this.EvalStaticMethod(Node, sMethod, new ZNode[] {Node.LeftNode(), Node.RightNode()});
	}

	@Override
	public void VisitAndNode(ZAndNode Node) {
		Object BooleanValue = this.Eval(Node.LeftNode());
		if(BooleanValue instanceof Boolean) {
			if((Boolean)BooleanValue) {
				this.EvaledValue = this.Eval(Node.RightNode());
			}
			else {
				this.EvaledValue = false;
			}
		}
	}

	@Override
	public void VisitOrNode(ZOrNode Node) {
		Object BooleanValue = this.Eval(Node.LeftNode());
		if(BooleanValue instanceof Boolean) {
			if(!(Boolean)BooleanValue) {
				this.EvaledValue = this.Eval(Node.RightNode());
			}
			else {
				this.EvaledValue = true;
			}
		}
	}

	@Override
	public void VisitBlockNode(ZBlockNode Node) {
		int i = 1;
		while(i < Node.GetListSize() && this.IsVisitable()) {
			ZNode StmtNode = Node.GetListAt(i);
			this.Eval(StmtNode);
			if(StmtNode.IsBreakingBlock()) {
				break;
			}
		}
		if(this.IsVisitable()) {
			this.EvaledValue = ZEmptyValue._TrueEmpty;
		}
	}

	@Override
	public void VisitVarNode(ZVarNode Node) {
		//TODO
		
	}

	@Override
	public void VisitIfNode(ZIfNode Node) {
		Object BooleanValue = this.Eval(Node.CondNode());
		if(BooleanValue instanceof Boolean) {
			if((Boolean)BooleanValue) {
				this.Eval(Node.ThenNode());
			}
			else if(Node.ElseNode() != null) {
				this.Eval(Node.ThenNode());
			}
		}
		if(this.IsVisitable()) {
			this.EvaledValue = ZEmptyValue._TrueEmpty;
		}
	}

	@Override
	public void VisitReturnNode(ZReturnNode Node) {
		//TODO
		
	}

	@Override
	public void VisitWhileNode(ZWhileNode Node) {
		//TODO
		
	}

	@Override
	public void VisitBreakNode(ZBreakNode Node) {
		//TODO
		
	}

	@Override
	public void VisitThrowNode(ZThrowNode Node) {
		//TODO
		
	}

	@Override
	public void VisitTryNode(ZTryNode Node) {
		//TODO
		
	}

	@Override
	public void VisitLetNode(ZLetNode Node) {
		if(Node.HasUntypedNode()) {
			LibZen._PrintDebug("HasUntypedNode: " + Node.HasUntypedNode() + "\n" + Node);
		}
		Node.Accept(this.generator);
	}

	@Override
	public void VisitFunctionNode(ZFunctionNode Node) {
		if(Node.HasUntypedNode()) {
			LibZen._PrintDebug("HasUntypedNode: " + Node.HasUntypedNode() + "\nLAZY: " + Node);
			this.generator.LazyBuild(Node);
		}
		else {
			Node.Accept(this.generator);
		}
	}

	@Override
	public void VisitClassNode(ZClassNode Node) {
		if(Node.HasUntypedNode()) {
			LibZen._PrintDebug("HasUntypedNode: " + Node.HasUntypedNode() + "\n" + Node);
		}
		Node.Accept(this.generator);
	}

	private void VisitStaticFieldNode(JavaStaticFieldNode Node) {
		try {
			Field f = Node.StaticClass.getField(Node.FieldName);
			this.EvaledValue = f.get(null);
		} catch (Exception e) {
			ZLogger._LogError(Node.SourceToken, "unresolved symbol: " + e);
			this.StopVisitor();
		}
	}

	@Override
	public void VisitAsmNode(ZAsmNode Node) {
		//TODO
		
	}

	@Override
	public void VisitErrorNode(ZErrorNode Node) {
		String Message = ZLogger._LogError(Node.SourceToken, Node.ErrorMessage);
		JavaOperatorApi.ThrowError(Message);
	}

	@Override
	public void VisitTopLevelNode(ZTopLevelNode Node) {
		//TODO
		
	}

	@Override
	public void VisitLocalDefinedNode(ZLocalDefinedNode Node) {
		if(Node instanceof JavaStaticFieldNode) {
			this.VisitStaticFieldNode((JavaStaticFieldNode)Node);
		}
		else {
			this.generator.VisitUndefinedNode(Node);
		}
	}

	@Override
	public boolean StartCodeGeneration(ZNode Node, boolean IsInteractive) {
		//TODO
		return false;
	}
}
