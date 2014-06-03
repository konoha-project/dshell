package dshell.internal.codegen;

import java.util.Stack;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import dshell.internal.codegen.ClassBuilder.MethodBuilder;
import dshell.internal.lib.DShellClassLoader;
import dshell.internal.parser.Node.ArrayNode;
import dshell.internal.parser.Node.AssertNode;
import dshell.internal.parser.Node.AssignNode;
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
import dshell.internal.parser.TypeUtils;

/**
 * generate java byte code from node.
 * @author skgchxngsxyz-osx
 *
 */
public class JavaByteCodeGen implements NodeVisitor<Object> {	//TODO: line number
	protected final DShellClassLoader classLoader;
	protected final Stack<ClassBuilder> classBuilders;
	protected final Stack<MethodBuilder> methodBuilders;

	public JavaByteCodeGen(DShellClassLoader classLoader) {
		this.classLoader = classLoader;
		this.classBuilders = new Stack<>();
		this.methodBuilders = new Stack<>();
	}

	public static byte[] generateFuncTypeInterface(FunctionType funcType) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		writer.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE, funcType.getInternalName(), null, "dshell/lang/Function", null);
		// generate method stub
		GeneratorAdapter adapter = new GeneratorAdapter(Opcodes.ACC_PUBLIC, funcType.getHandle().getMethodDesc(), null, null, writer);
		adapter.endMethod();
		// generate static field containing FuncType name
		String fieldDesc = org.objectweb.asm.Type.getType(String.class).getDescriptor();
		writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "funcTypeName", fieldDesc, null, funcType.getTypeName());
		writer.visitEnd();
		return writer.toByteArray();
	}

	private ClassBuilder getCurrentClassBuilder() {
		return this.classBuilders.peek();
	}

	private MethodBuilder getCurrentMethodBuilder() {
		return this.methodBuilders.peek();
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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
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
	public Void visit(FuncCallNode node) {	// TODO: invokestatic or invokeinterface
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
		for(Node statementNode : node.getNodeList()) {
			statementNode.accept(this);
			if((statementNode instanceof ExprNode) && ((ExprNode)statementNode).isStatement()) {
				this.getCurrentMethodBuilder().pop(TypeUtils.toTypeDescriptor(((ExprNode) statementNode).getType()));
			}
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(ForInNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(WhileNode node) {
		// TODO Auto-generated method stub
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
		node.getThenBlockNode().accept(this);
		adapter.goTo(mergeLabel);
		// else block
		adapter.mark(elseLabel);
		node.getElseBlockNode().accept(this);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(CatchNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(VarDeclNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(AssignNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visit(FunctionNode node) {
		// TODO Auto-generated method stub
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
		for(Node statementNode : node.getNodeList()) {
			statementNode.accept(this);
			if((statementNode instanceof ExprNode) && !((ExprNode)statementNode).isStatement()) {
				GeneratorAdapter adapter = this.getCurrentMethodBuilder();
				Type type = ((ExprNode)statementNode).getType();
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
