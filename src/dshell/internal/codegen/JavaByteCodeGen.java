package dshell.internal.codegen;

import java.util.ArrayList;
import java.util.Stack;

import org.antlr.v4.runtime.atn.SemanticContext.OR;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import dshell.internal.codegen.ClassBuilder.MethodBuilder;
import dshell.internal.codegen.ClassBuilder.TryBlockLabels;
import dshell.internal.lib.DShellClassLoader;
import dshell.internal.parser.CalleeHandle.MethodHandle;
import dshell.internal.parser.CalleeHandle.OperatorHandle;
import dshell.internal.parser.CalleeHandle.StaticFieldHandle;
import dshell.internal.parser.CalleeHandle.StaticFunctionHandle;
import dshell.internal.parser.Node.ArrayNode;
import dshell.internal.parser.Node.AssertNode;
import dshell.internal.parser.Node.AssignNode;
import dshell.internal.parser.Node.AssignableNode;
import dshell.internal.parser.Node.BlockNode;
import dshell.internal.parser.Node.BooleanValueNode;
import dshell.internal.parser.Node.BreakNode;
import dshell.internal.parser.Node.CastNode;
import dshell.internal.parser.Node.CatchNode;
import dshell.internal.parser.Node.ClassNode;
import dshell.internal.parser.Node.CondOpNode;
import dshell.internal.parser.Node.ConstructorCallNode;
import dshell.internal.parser.Node.ConstructorNode;
import dshell.internal.parser.Node.ContinueNode;
import dshell.internal.parser.Node.ElementGetterNode;
import dshell.internal.parser.Node.EmptyBlockNode;
import dshell.internal.parser.Node.EmptyNode;
import dshell.internal.parser.Node.ExportEnvNode;
import dshell.internal.parser.Node.ExprNode;
import dshell.internal.parser.Node.FieldGetterNode;
import dshell.internal.parser.Node.FloatValueNode;
import dshell.internal.parser.Node.ForInNode;
import dshell.internal.parser.Node.ForNode;
import dshell.internal.parser.Node.FuncCallNode;
import dshell.internal.parser.Node.FunctionNode;
import dshell.internal.parser.Node.IfNode;
import dshell.internal.parser.Node.ImportEnvNode;
import dshell.internal.parser.Node.InstanceofNode;
import dshell.internal.parser.Node.IntValueNode;
import dshell.internal.parser.Node.MapNode;
import dshell.internal.parser.Node.MethodCallNode;
import dshell.internal.parser.Node.NullNode;
import dshell.internal.parser.Node.OperatorCallNode;
import dshell.internal.parser.Node.ReturnNode;
import dshell.internal.parser.Node.RootNode;
import dshell.internal.parser.Node.StringValueNode;
import dshell.internal.parser.Node.SuffixIncrementNode;
import dshell.internal.parser.Node.SymbolNode;
import dshell.internal.parser.Node.ThrowNode;
import dshell.internal.parser.Node.TryNode;
import dshell.internal.parser.Node.VarDeclNode;
import dshell.internal.parser.Node.WhileNode;
import dshell.internal.parser.TypePool.FunctionType;
import dshell.internal.parser.TypePool.GenericArrayType;
import dshell.internal.parser.TypePool.GenericType;
import dshell.internal.parser.TypePool.PrimitiveType;
import dshell.internal.parser.TypePool.Type;
import dshell.internal.parser.Node;
import dshell.internal.parser.NodeVisitor;
import dshell.internal.parser.TypePool.VoidType;
import dshell.internal.parser.TypeUtils;

/**
 * generate java byte code from node.
 * @author skgchxngsxyz-osx
 *
 */
public class JavaByteCodeGen implements NodeVisitor<Object> {	//TODO: line number
	protected final DShellClassLoader classLoader;
	protected final Stack<MethodBuilder> methodBuilders;

	public JavaByteCodeGen(DShellClassLoader classLoader) {
		this.classLoader = classLoader;
		this.methodBuilders = new Stack<>();
	}

	public static byte[] generateFuncTypeInterface(FunctionType funcType) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		writer.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, funcType.getInternalName(), null, "java/lang/Object", null);
		// generate method stub
		GeneratorAdapter adapter = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, funcType.getHandle().getMethodDesc(), null, null, writer);
		adapter.endMethod();
		// generate static field containing FuncType name
		String fieldDesc = org.objectweb.asm.Type.getType(String.class).getDescriptor();
		writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "funcTypeName", fieldDesc, null, funcType.getTypeName());
		writer.visitEnd();
		return writer.toByteArray();
	}

	private MethodBuilder getCurrentMethodBuilder() {
		return this.methodBuilders.peek();
	}

	private void visitBlockWithCurrentScope(BlockNode blockNode) {
		blockNode.accept(this);
	}

	private void visitBlockWithNewScope(BlockNode blockNode) {
		this.getCurrentMethodBuilder().createNewLocalScope();
		this.visitBlockWithCurrentScope(blockNode);
		this.getCurrentMethodBuilder().removeCurrentLocalScope();
	}

	private void createPopInsIfExprNode(Node node) {
		if(!(node instanceof ExprNode)) {
			return;
		}
		Type type = ((ExprNode) node).getType();
		if(type instanceof VoidType) {
			return;
		}
		this.getCurrentMethodBuilder().pop(TypeUtils.toTypeDescriptor(type));
	}

	@Override
	public Void visit(IntValueNode node) {
		this.getCurrentMethodBuilder().push(node.getValue());
		return null;
	}

	@Override
	public Void visit(FloatValueNode node) {
		this.getCurrentMethodBuilder().push(node.getValue());
		return null;
	}

	@Override
	public Void visit(BooleanValueNode node) {
		this.getCurrentMethodBuilder().push(node.getValue());
		return null;
	}

	@Override
	public Void visit(StringValueNode node) {
		this.getCurrentMethodBuilder().push(node.getValue());
		return null;
	}

	@Override
	public Void visit(NullNode node) {
		this.getCurrentMethodBuilder().visitInsn(Opcodes.ACONST_NULL);
		return null;
	}

	@Override
	public Void visit(ArrayNode node) {	//TODO: array consturctor
		int size = node.getNodeList().size();
		Type elementType = ((GenericArrayType) node.getType()).getElementTypeList().get(0);
		org.objectweb.asm.Type elementTypeDesc = TypeUtils.toTypeDescriptor(elementType);
		org.objectweb.asm.Type arrayClassDesc = TypeUtils.toTypeDescriptor(node.getType().getInternalName());

		GeneratorAdapter adapter = this.getCurrentMethodBuilder();
		adapter.newInstance(arrayClassDesc);
		adapter.push(size);
		adapter.newArray(elementTypeDesc);
		adapter.dup();
		for(int i = 0; i < size; i++) {
			adapter.push(i);
			node.getNodeList().get(i).accept(this);
			adapter.arrayStore(elementTypeDesc);
		}
		//adapter.invokeConstructor(arrayClassDesc, arg1);
		return null;
	}

	@Override
	public Void visit(MapNode node) {	//TODO: map constructor
		int size = node.getKeyList().size();
		Type elementType = ((GenericType) node.getType()).getElementTypeList().get(0);
		org.objectweb.asm.Type keyTypeDesc = TypeUtils.toTypeDescriptor("java/lang/String");
		org.objectweb.asm.Type valueTypeDesc = TypeUtils.toTypeDescriptor(elementType);
		org.objectweb.asm.Type mapClassDesc = TypeUtils.toTypeDescriptor(node.getType().getInternalName());

		GeneratorAdapter adapter = this.getCurrentMethodBuilder();
		adapter.newInstance(mapClassDesc);
		// generate key array
		adapter.push(size);
		adapter.newArray(keyTypeDesc);
		adapter.dup();
		for(int i = 0; i < size; i++) {
			adapter.push(i);
			node.getKeyList().get(i).accept(this);
			adapter.arrayStore(keyTypeDesc);
		}
		// generate value array
		adapter.push(size);
		adapter.newArray(valueTypeDesc);
		adapter.dup();
		for(int i = 0; i < size; i++) {
			adapter.push(i);
			node.getValueList().get(i).accept(this);
			adapter.arrayStore(valueTypeDesc);
		}
		//adapter.invokeConstructor(mapClassDesc, null);
		return null;
	}

	@Override
	public Void visit(SymbolNode node) {
		// get func object from static field
		StaticFieldHandle handle = node.getHandle();
		if(handle != null) {
			handle.callGetter(this.getCurrentMethodBuilder());
			return null;
		}
		MethodBuilder mBuilder = this.getCurrentMethodBuilder();
		mBuilder.loadValueFromLocalVar(node.getSymbolName(), node.getType());
		return null;
	}

	@Override
	public Void visit(ElementGetterNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(FieldGetterNode node) {
		node.getRecvNode().accept(this);
		node.getHandle().callGetter(this.getCurrentMethodBuilder());
		return null;
	}

	@Override
	public Void visit(CastNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(InstanceofNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(SuffixIncrementNode node) {
		long incSize = node.getOperator().equals("++") ? 1 : -1;
		MethodBuilder mBuilder = this.getCurrentMethodBuilder(); 
		SymbolNode symbolNode = node.getSymbolNode();
		symbolNode.accept(this);
		mBuilder.dup2();
		Type symbolType = symbolNode.getType();
		org.objectweb.asm.Type typeDesc = TypeUtils.toTypeDescriptor(symbolType);
		if(typeDesc.equals(org.objectweb.asm.Type.DOUBLE_TYPE)) {
			mBuilder.push((double) incSize);
		} else {
			mBuilder.push(incSize);
		}
		mBuilder.math(GeneratorAdapter.ADD, typeDesc);
		mBuilder.storeValueToLocalVar(symbolNode.getSymbolName(), symbolType);
		return null;
	}

	@Override
	public Void visit(OperatorCallNode node) {
		for(Node paramNode : node.getNodeList()) {
			paramNode.accept(this);
		}
		node.getHandle().call(this.getCurrentMethodBuilder());
		return null;
	}

	@Override
	public Void visit(FuncCallNode node) {
		MethodHandle handle = node.getHandle();
		if(!(handle instanceof StaticFunctionHandle)) {
			this.getCurrentMethodBuilder().loadValueFromLocalVar(node.getFuncName(), node.getHandle().getOwnerType());
		}
		for(Node paramNode : node.getNodeList()) {
			paramNode.accept(this);
		}
		node.getHandle().call(this.getCurrentMethodBuilder());
		return null;
	}

	@Override
	public Void visit(MethodCallNode node) {
		node.getRecvNode().accept(this);
		for(Node paramNode : node.getNodeList()) {
			paramNode.accept(this);
		}
		node.getHandle().call(this.getCurrentMethodBuilder());
		return null;
	}

	@Override
	public Void visit(ConstructorCallNode node) {
		Type revType = node.getHandle().getOwnerType();
		GeneratorAdapter adapter = this.getCurrentMethodBuilder();
		adapter.newInstance(TypeUtils.toTypeDescriptor(revType));
		adapter.dup();
		for(Node paramNode : node.getNodeList()) {
			paramNode.accept(this);
		}
		node.getHandle().call(adapter);
		return null;
	}

	@Override
	public Void visit(CondOpNode node) {
		GeneratorAdapter adapter = this.getCurrentMethodBuilder();
		// generate and.
		if(node.getConditionalOp().equals("&&")) {
			Label rightLabel = adapter.newLabel();
			Label mergeLabel = adapter.newLabel();
			// and left
			node.getLeftNode().accept(this);
			adapter.push(true);
			adapter.ifCmp(org.objectweb.asm.Type.BOOLEAN_TYPE, GeneratorAdapter.EQ, rightLabel);
			adapter.push(false);
			adapter.goTo(mergeLabel);
			// and right
			adapter.mark(rightLabel);
			node.getRightNode().accept(this);
			adapter.mark(mergeLabel);
		// generate or
		} else {
			Label rightLabel = adapter.newLabel();
			Label mergeLabel = adapter.newLabel();
			// or left
			node.getLeftNode().accept(this);
			adapter.push(true);
			adapter.ifCmp(org.objectweb.asm.Type.BOOLEAN_TYPE, GeneratorAdapter.NE, rightLabel);
			adapter.push(true);
			adapter.goTo(mergeLabel);
			// or right
			adapter.mark(rightLabel);
			node.getRightNode().accept(this);
			adapter.mark(mergeLabel);
		}
		return null;
	}

	@Override
	public Void visit(AssertNode node) {
		node.getExprNode().accept(this);
		node.getHandle().call(this.getCurrentMethodBuilder());
		return null;
	}

	@Override
	public Void visit(BlockNode node) {
		for(Node targetNode : node.getNodeList()) {
			targetNode.accept(this);
			this.createPopInsIfExprNode(targetNode);
		}
		return null;
	}

	@Override
	public Void visit(BreakNode node) {
		Label label = this.getCurrentMethodBuilder().getBreakLabels().peek();
		this.getCurrentMethodBuilder().goTo(label);
		return null;
	}

	@Override
	public Void visit(ContinueNode node) {
		Label label = this.getCurrentMethodBuilder().getContinueLabels().peek();
		this.getCurrentMethodBuilder().goTo(label);
		return null;
	}

	@Override
	public Void visit(ExportEnvNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(ImportEnvNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(ForNode node) {
		// init label
		MethodBuilder mBuilder = this.getCurrentMethodBuilder();
		Label continueLabel = mBuilder.newLabel();
		Label breakLabel = mBuilder.newLabel();
		mBuilder.continueLabels.push(continueLabel);
		mBuilder.breakLabels.push(breakLabel);

		mBuilder.createNewLocalScope();
		// init
		node.getInitNode().accept(this);
		this.createPopInsIfExprNode(node.getInitNode());
		// cond
		mBuilder.mark(continueLabel);
		mBuilder.push(true);
		node.getCondNode().accept(this);
		mBuilder.ifCmp(org.objectweb.asm.Type.BOOLEAN_TYPE, GeneratorAdapter.NE, breakLabel);
		// block
		this.visitBlockWithCurrentScope(node.getBlockNode());
		// iter
		node.getIterNode().accept(this);
		this.createPopInsIfExprNode(node.getIterNode());
		mBuilder.goTo(continueLabel);
		mBuilder.mark(breakLabel);

		mBuilder.removeCurrentLocalScope();
		// remove label
		mBuilder.continueLabels.pop();
		mBuilder.breakLabels.pop();
		return null;
	}

	@Override
	public Void visit(ForInNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(WhileNode node) {
		// init label
		MethodBuilder mBuilder = this.getCurrentMethodBuilder();
		Label continueLabel = mBuilder.newLabel();
		Label breakLabel = mBuilder.newLabel();
		mBuilder.continueLabels.push(continueLabel);
		mBuilder.breakLabels.push(breakLabel);

		mBuilder.mark(continueLabel);
		mBuilder.push(true);
		node.getCondNode().accept(this);
		mBuilder.ifCmp(org.objectweb.asm.Type.BOOLEAN_TYPE, GeneratorAdapter.NE, breakLabel);
		this.visitBlockWithNewScope(node.getBlockNode());
		mBuilder.goTo(continueLabel);
		mBuilder.mark(breakLabel);

		// remove label
		mBuilder.continueLabels.pop();
		mBuilder.breakLabels.pop();
		return null;
	}

	@Override
	public Void visit(IfNode node) {
		GeneratorAdapter adapter = this.getCurrentMethodBuilder();
		Label elseLabel = adapter.newLabel();
		Label mergeLabel = adapter.newLabel();
		// if cond
		node.getCondNode().accept(this);
		adapter.push(true);
		adapter.ifCmp(org.objectweb.asm.Type.BOOLEAN_TYPE, GeneratorAdapter.NE, elseLabel);
		// then block
		this.visitBlockWithNewScope(node.getThenBlockNode());
		adapter.goTo(mergeLabel);
		// else block
		adapter.mark(elseLabel);
		this.visitBlockWithNewScope(node.getElseBlockNode());
		adapter.mark(mergeLabel);
		return null;
	}

	@Override
	public Void visit(ReturnNode node) {
		node.getExprNode().accept(this);
		this.getCurrentMethodBuilder().returnValue();
		return null;
	}

	@Override
	public Void visit(ThrowNode node) {
		node.getExprNode().accept(this);
		this.getCurrentMethodBuilder().throwException();
		return null;
	}

	@Override
	public Void visit(TryNode node) {
		// init labels
		MethodBuilder mBuilder = this.getCurrentMethodBuilder();
		TryBlockLabels labels = mBuilder.createNewTryLabel();
		mBuilder.getTryLabels().push(labels);
		// try block
		mBuilder.mark(labels.startLabel);
		this.visitBlockWithNewScope(node.getTryBlockNode());
		mBuilder.mark(labels.endLabel);
		mBuilder.goTo(labels.finallyLabel);
		// catch block
		for(CatchNode catchNode : node.getCatchNodeList()) {
			catchNode.accept(this);
		}
		// finally block
		mBuilder.mark(labels.finallyLabel);
		this.visitBlockWithNewScope(node.getFinallyBlockNode());
		mBuilder.getTryLabels().pop();
		return null;
	}

	@Override
	public Void visit(CatchNode node) {
		MethodBuilder mBuilder = this.getCurrentMethodBuilder();
		TryBlockLabels labels = mBuilder.getTryLabels().peek();
		mBuilder.createNewLocalScope();
		Type exceptionType = node.getExceptionType();
		mBuilder.catchException(labels.startLabel, labels.endLabel, TypeUtils.toTypeDescriptor(exceptionType));
		mBuilder.createNewLocalVarAndStoreValue(node.getExceptionVarName(), exceptionType);
		this.visitBlockWithCurrentScope(node.getCatchBlockNode());
		mBuilder.goTo(labels.finallyLabel);
		mBuilder.removeCurrentLocalScope();
		return null;
	}

	@Override
	public Void visit(VarDeclNode node) {
		MethodBuilder mBuilder = this.getCurrentMethodBuilder();
		node.getInitValueNode().accept(this);
		mBuilder.createNewLocalVarAndStoreValue(node.getVarName(), node.getInitValueNode().getType());
		return null;
	}

	@Override
	public Void visit(AssignNode node) {
		MethodBuilder mBuilder = this.getCurrentMethodBuilder();
		OperatorHandle handle = node.getHandle();
		if(handle != null) {	// self assgin
			node.getLeftNode().accept(this);
			node.getRightNode().accept(this);
			handle.call(mBuilder);
		} else {
			node.getRightNode().accept(this);
		}
		AssignableNode leftNode = node.getLeftNode();
		if(leftNode instanceof SymbolNode) {
			this.visitAssignLeft((SymbolNode)leftNode);
		} else if(leftNode instanceof FieldGetterNode) {
			this.visitAssignLeft((FieldGetterNode)leftNode);
		} else if(leftNode instanceof ElementGetterNode) {
			this.visitAssignLeft((ElementGetterNode)leftNode);
		}
		return null;
	}

	private void visitAssignLeft(SymbolNode leftNode) {	//TODO: global variable
		MethodBuilder mBuilder = this.getCurrentMethodBuilder();
		mBuilder.storeValueToLocalVar(leftNode.getSymbolName(), leftNode.getType());
	}

	private void visitAssignLeft(FieldGetterNode leftNode) {
		leftNode.getHandle().callSetter(this.getCurrentMethodBuilder());
	}

	private void visitAssignLeft(ElementGetterNode leftNode) {	//TODO:
		
	}

	@Override
	public Void visit(FunctionNode node) {
		ClassBuilder classBuilder = new ClassBuilder(node.getHolderType(), null);
		// create static field.
		StaticFieldHandle fieldHandle = node.getHolderType().getFieldHandle();
		org.objectweb.asm.Type fieldTypeDesc = TypeUtils.toTypeDescriptor(fieldHandle.getFieldType());
		classBuilder.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, fieldHandle.getCalleeName(), fieldTypeDesc.getDescriptor(), null, null);

		// generate static method.
		MethodBuilder mBuilder = classBuilder.createNewMethodBuilder(node.getHolderType().getFuncHanle());
		this.methodBuilders.push(mBuilder);
		mBuilder.createNewLocalScope();
		// set argument decl
		int size = node.getArgDeclNodeList().size();
		for(int i = 0; i < size; i++) {
			SymbolNode argNode = node.getArgDeclNodeList().get(i);
			Type argType = node.getHolderType().getFuncHanle().getParamTypeList().get(i);
			mBuilder.defineArgument(argNode.getSymbolName(), argType);
		}
		this.visitBlockWithCurrentScope(node.getBlockNode());
		mBuilder.removeCurrentLocalScope();
		this.methodBuilders.pop().endMethod();

		// generate interface method
		MethodHandle handle = ((FunctionType)node.getHolderType().getFieldHandle().getFieldType()).getHandle();
		mBuilder = classBuilder.createNewMethodBuilder(handle);
		mBuilder.loadArgs();
		node.getHolderType().getFuncHanle().call(mBuilder);
		mBuilder.returnValue();
		mBuilder.endMethod();

		// generate constructor.
		org.objectweb.asm.commons.Method initDesc = TypeUtils.toConstructorDescriptor(new ArrayList<Type>());
		GeneratorAdapter adapter = new GeneratorAdapter(Opcodes.ACC_PUBLIC, initDesc, null, null, classBuilder);
		adapter.loadThis();
		adapter.invokeConstructor(org.objectweb.asm.Type.getType("java/lang/Object"), initDesc);
		adapter.returnValue();
		adapter.endMethod();

		// generate static initializer
		org.objectweb.asm.commons.Method cinitDesc = org.objectweb.asm.commons.Method.getMethod("void <clinit> ()");
		adapter = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, cinitDesc, null, null, classBuilder);
		org.objectweb.asm.Type ownerType = TypeUtils.toTypeDescriptor(fieldHandle.getOwnerType());
		adapter.newInstance(ownerType);
		adapter.dup();
		adapter.invokeConstructor(ownerType, initDesc);
		fieldHandle.callSetter(adapter);
		adapter.returnValue();
		adapter.endMethod();

		classBuilder.generateClass(this.classLoader);
		return null;
	}

	@Override
	public Void visit(ClassNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(ConstructorNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(EmptyNode node) {	//do nothing
		return null;
	}

	@Override
	public Void visit(EmptyBlockNode node) {	// do nothing
		return null;
	}

	public Class<?> generateTopLevelClass(RootNode node, boolean isInteractive) {	//TODO: 
		if(!isInteractive) {
			return null;
		}
		ClassBuilder classBuilder = new ClassBuilder();
		this.methodBuilders.push(classBuilder.createNewMethodBuilder(null));
		for(Node targetNode : node.getNodeList()) {
			targetNode.accept(this);
			if((targetNode instanceof ExprNode) && !(((ExprNode)targetNode).getType() instanceof VoidType)) {
				GeneratorAdapter adapter = this.getCurrentMethodBuilder();
				Type type = ((ExprNode)targetNode).getType();
				if(type instanceof PrimitiveType) {
					adapter.box(TypeUtils.toTypeDescriptor(type));
				}
				adapter.push(type.getTypeName());
				node.getHandle().call(adapter);
			}
		}
		this.methodBuilders.peek().returnValue();
		this.methodBuilders.pop().endMethod();
		return classBuilder.generateClass(this.classLoader.createChild());
	}
}
