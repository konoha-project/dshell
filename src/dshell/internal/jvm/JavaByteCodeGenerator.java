// ***************************************************************************
// Copyright (c) 2013, JST/CREST DEOS project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// *  Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// *  Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// **************************************************************************

package dshell.internal.jvm;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Stack;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.GroupNode;
import libbun.ast.LocalDefinedNode;
import libbun.ast.binary.AssignNode;
import libbun.ast.binary.BinaryOperatorNode;
import libbun.ast.binary.BunAddNode;
import libbun.ast.binary.BunAndNode;
import libbun.ast.binary.BunBitwiseAndNode;
import libbun.ast.binary.BunBitwiseOrNode;
import libbun.ast.binary.BunBitwiseXorNode;
import libbun.ast.binary.BunDivNode;
import libbun.ast.binary.BunEqualsNode;
import libbun.ast.binary.BunGreaterThanEqualsNode;
import libbun.ast.binary.BunGreaterThanNode;
import libbun.ast.binary.BunInstanceOfNode;
import libbun.ast.binary.BunLeftShiftNode;
import libbun.ast.binary.BunLessThanEqualsNode;
import libbun.ast.binary.BunLessThanNode;
import libbun.ast.binary.BunModNode;
import libbun.ast.binary.BunMulNode;
import libbun.ast.binary.BunNotEqualsNode;
import libbun.ast.binary.BunOrNode;
import libbun.ast.binary.BunRightShiftNode;
import libbun.ast.binary.BunSubNode;
import libbun.ast.decl.BunClassNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.BunVarBlockNode;
import libbun.ast.decl.TopLevelNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.expression.BunFormNode;
import libbun.ast.expression.BunFuncNameNode;
import libbun.ast.expression.FuncCallNode;
import libbun.ast.expression.GetFieldNode;
import libbun.ast.expression.GetIndexNode;
import libbun.ast.expression.GetNameNode;
import libbun.ast.expression.MethodCallNode;
import libbun.ast.expression.NewObjectNode;
import libbun.ast.literal.BunArrayLiteralNode;
import libbun.ast.literal.BunAsmNode;
import libbun.ast.literal.BunBooleanNode;
import libbun.ast.literal.BunFloatNode;
import libbun.ast.literal.BunIntNode;
import libbun.ast.literal.BunMapEntryNode;
import libbun.ast.literal.BunMapLiteralNode;
import libbun.ast.literal.BunNullNode;
import libbun.ast.literal.BunStringNode;
import libbun.ast.literal.BunTypeNode;
import libbun.ast.literal.LiteralNode;
import libbun.ast.statement.BunBreakNode;
import libbun.ast.statement.BunIfNode;
import libbun.ast.statement.BunReturnNode;
import libbun.ast.statement.BunThrowNode;
import libbun.ast.statement.BunTryNode;
import libbun.ast.statement.BunWhileNode;
import libbun.ast.unary.BunCastNode;
import libbun.ast.unary.BunComplementNode;
import libbun.ast.unary.BunMinusNode;
import libbun.ast.unary.BunNotNode;
import libbun.ast.unary.BunPlusNode;
import libbun.ast.unary.UnaryOperatorNode;
import libbun.encode.LibBunGenerator;
import libbun.lang.bun.BunTypeSafer;
import libbun.parser.classic.BSourceContext;
import libbun.parser.classic.BTokenContext;
//import libbun.parser.classic.LibBunGamma;
import libbun.parser.classic.LibBunLangInfo;
import libbun.parser.classic.LibBunLogger;
import libbun.type.BClassField;
import libbun.type.BClassType;
import libbun.type.BFunc;
import libbun.type.BFuncType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.util.BArray;
import libbun.util.BFunction;
import libbun.util.BMatchFunction;
import libbun.util.BTokenFunction;
import libbun.util.BunMap;
import libbun.util.BunObject;
import libbun.util.LibBunSystem;
import libbun.util.SoftwareFault;
import libbun.util.Var;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class JavaByteCodeGenerator extends LibBunGenerator {
	protected final JavaTypeTable typeTable;
	protected final JavaMethodTable methodTable;
	public final JavaTypeUtils javaTypeUtils;
	protected final BunMap<Class<?>> GeneratedClassMap;
	protected final BunMap<BNode> LazyNodeMap;
	public JavaStaticFieldNode MainFuncNode = null;
	GeneratedClassLoader AsmLoader = null;
	Stack<TryCatchLabel> TryCatchLabel;
	MethodBuilder AsmBuilder;

	public JavaByteCodeGenerator() {
		super(new LibBunLangInfo("Java1.6", "jvm"));
		this.typeTable = new JavaTypeTable();
		this.methodTable = new JavaMethodTable(this.typeTable);
		this.GeneratedClassMap = new BunMap<Class<?>>(null);
		this.LazyNodeMap = new BunMap<BNode>(null);
		this.javaTypeUtils = new JavaTypeUtils(this, this.typeTable);
		this.InitFuncClass();
		//this.ImportLocalGrammar(this.RootGamma);
		this.TryCatchLabel = new Stack<TryCatchLabel>();
		this.AsmLoader = new GeneratedClassLoader(this);
	}

//	private void ImportLocalGrammar(LibBunGamma Gamma) {
//		Gamma.DefineStatement("import", new JavaImportPattern());
//		Gamma.DefineExpression("$JavaClassPath$", new JavaClassPathPattern());
//	}

	private void InitFuncClass() {
		BFuncType FuncType = this.typeTable.FuncType(boolean.class, BSourceContext.class);
		this.SetGeneratedClass(this.NameType(FuncType), BTokenFunction.class);
		FuncType = this.typeTable.FuncType(BNode.class, BNode.class, BTokenContext.class, BNode.class);
		this.SetGeneratedClass(this.NameType(FuncType), BMatchFunction.class);
	}

	private final void SetGeneratedClass(String Key, Class<?> C) {
		this.GeneratedClassMap.put(Key, C);
	}

	private final Class<?> GetGeneratedClass(String Key, Class<?> DefaultClass) {
		Class<?> C = this.GeneratedClassMap.GetOrNull(Key);
		if(C != null) {
			return C;
		}
		return DefaultClass;
	}

	public Class<?> GetDefinedFunctionClass(String FuncName, BFuncType FuncType) {
		return this.GeneratedClassMap.GetOrNull(this.NameFunctionClass(FuncName, FuncType));
	}

	public Class<?> GetDefinedFunctionClass(String FuncName, BType RecvType, int FuncParamSize) {
		return this.GeneratedClassMap.GetOrNull(this.NameFunctionClass(FuncName, RecvType, FuncParamSize));
	}

	protected void LazyBuild(BunFunctionNode Node) {
		this.LazyNodeMap.put(Node.GetSignature(), Node);
	}

	protected void LazyBuild(String Signature) {
		BNode Node = this.LazyNodeMap.GetOrNull(Signature);
		if(Node != null) {
			LibBunSystem._PrintDebug("LazyBuilding: " + Signature);
			this.LazyNodeMap.remove(Signature);
			Node.Accept(this);
		}
	}

	public final Class<?> GetJavaClass(BType zType, Class<?> C) {
		if(zType instanceof BFuncType) {
			return this.LoadFuncClass((BFuncType)zType);
		}
		return this.typeTable.GetJavaClass(zType, C);
	}

	public final Class<?> GetJavaClass(BType zType) {
		return this.GetJavaClass(zType, Object.class);
	}

	@Override public BType GetFieldType(BType RecvType, String FieldName) {
		Class<?> NativeClass = this.GetJavaClass(RecvType);
		if(NativeClass != null) {
			try {
				java.lang.reflect.Field NativeField = NativeClass.getField(FieldName);
				if(Modifier.isPublic(NativeField.getModifiers())) {
					return this.typeTable.GetBunType(NativeField.getType());
				}
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
			return BType.VoidType;     // undefined
		}
		return BType.VarType;     // undefined
	}

	@Override public BType GetSetterType(BType RecvType, String FieldName) {
		Class<?> NativeClass = this.GetJavaClass(RecvType);
		if(NativeClass != null) {
			try {
				java.lang.reflect.Field NativeField = NativeClass.getField(FieldName);
				if(Modifier.isPublic(NativeField.getModifiers()) && !Modifier.isFinal(NativeField.getModifiers())) {
					return this.typeTable.GetBunType(NativeField.getType());
				}
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
			return BType.VoidType;     // undefined
		}
		return BType.VarType;     // undefined
	}

	@Override public BFuncType GetMethodFuncType(BType RecvType, String MethodName, AbstractListNode ParamList) {
		if(MethodName == null) {
			Constructor<?> jMethod = this.javaTypeUtils.GetConstructor(RecvType, ParamList);
			if(jMethod != null) {
				@Var Class<?>[] ParamTypes = jMethod.getParameterTypes();
				@Var BArray<BType> TypeList = new BArray<BType>(new BType[ParamTypes.length + 2]);
				if (ParamTypes != null) {
					@Var int j = 0;
					while(j < LibBunSystem._Size(ParamTypes)) {
						TypeList.add(this.typeTable.GetBunType(ParamTypes[j]));
						j = j + 1;
					}
				}
				TypeList.add(RecvType);
				return BTypePool._LookupFuncType2(TypeList);
			}
		}
		else {
			Method jMethod = this.javaTypeUtils.GetMethod(RecvType, MethodName, ParamList);
			if(jMethod != null) {
				return this.typeTable.ConvertToFuncType(jMethod);
			}
		}
		return null;
	}

	@Override public void VisitNullNode(BunNullNode Node) {
		this.AsmBuilder.visitInsn(Opcodes.ACONST_NULL);
	}

	@Override public void VisitBooleanNode(BunBooleanNode Node) {
		this.AsmBuilder.pushBoolean(Node.BooleanValue);
	}

	@Override public void VisitIntNode(BunIntNode Node) {
		this.AsmBuilder.pushLong(Node.IntValue);
	}

	@Override public void VisitFloatNode(BunFloatNode Node) {
		this.AsmBuilder.pushDouble(Node.FloatValue);
	}

	@Override public void VisitStringNode(BunStringNode Node) {
		this.AsmBuilder.visitLdcInsn(Node.StringValue);
	}

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode Node) {
		if(Node.IsUntyped()) {
			this.VisitErrorNode(new ErrorNode(Node, "ambigious array"));
			return;
		}
		Class<?> ArrayClass = this.javaTypeUtils.AsArrayClass(Node.Type);
		String Owner = Type.getInternalName(ArrayClass);
		this.AsmBuilder.visitTypeInsn(NEW, Owner);
		this.AsmBuilder.visitInsn(DUP);
		this.AsmBuilder.pushInt(Node.Type.TypeId);
		this.AsmBuilder.oushNodeListAsArray(this.javaTypeUtils.AsElementClass(Node.Type), 0, Node);
		this.AsmBuilder.setLineNumber(Node);
		this.AsmBuilder.visitMethodInsn(INVOKESPECIAL, Owner, "<init>", this.javaTypeUtils.NewArrayDescriptor(Node.Type));
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode Node) {
		if(Node.IsUntyped()) {
			this.VisitErrorNode(new ErrorNode(Node, "ambigious map"));
			return;
		}
		String Owner = Type.getInternalName(BunMap.class);
		this.AsmBuilder.visitTypeInsn(NEW, Owner);
		this.AsmBuilder.visitInsn(DUP);
		this.AsmBuilder.pushInt(Node.Type.TypeId);
		this.AsmBuilder.pushInt(Node.GetListSize() * 2);
		this.AsmBuilder.visitTypeInsn(ANEWARRAY, Type.getInternalName(Object.class));
		for(int i = 0; i < Node.GetListSize() ; i++) {
			BunMapEntryNode EntryNode = Node.GetMapEntryNode(i);
			this.AsmBuilder.visitInsn(DUP);
			this.AsmBuilder.pushInt(i * 2);
			this.AsmBuilder.pushNode(String.class, EntryNode.KeyNode());
			this.AsmBuilder.visitInsn(Opcodes.AASTORE);
			this.AsmBuilder.visitInsn(DUP);
			this.AsmBuilder.pushInt(i * 2 + 1);
			this.AsmBuilder.pushNode(Object.class, EntryNode.ValueNode());
			this.AsmBuilder.visitInsn(Opcodes.AASTORE);
		}
		this.AsmBuilder.setLineNumber(Node);
		String Desc = Type.getMethodDescriptor(Type.getType(void.class), new Type[] { Type.getType(int.class),  Type.getType(Object[].class)});
		this.AsmBuilder.visitMethodInsn(INVOKESPECIAL, Owner, "<init>", Desc);
	}

	@Override public void VisitNewObjectNode(NewObjectNode Node) {
		if(Node.IsUntyped()) {
			this.VisitErrorNode(new ErrorNode(Node, "no class for new operator"));
			return;
		}
		// check class existence
		if(!Node.Type.Equals(this.typeTable.GetBunType(this.GetJavaClass(Node.Type)))) {
			this.VisitErrorNode(new ErrorNode(Node, "undefined class: " + Node.Type));
			return;
		}
		String ClassName = Type.getInternalName(this.GetJavaClass(Node.Type));
		this.AsmBuilder.visitTypeInsn(NEW, ClassName);
		this.AsmBuilder.visitInsn(DUP);
		Constructor<?> jMethod = this.javaTypeUtils.GetConstructor(Node.Type, Node);
		if(jMethod != null) {
			Class<?>[] P = jMethod.getParameterTypes();
			for(int i = 0; i < P.length; i++) {
				this.AsmBuilder.pushNode(P[i], Node.GetListAt(i));
			}
			this.AsmBuilder.setLineNumber(Node);
			this.AsmBuilder.visitMethodInsn(INVOKESPECIAL, ClassName, "<init>", Type.getConstructorDescriptor(jMethod));
		}
		else {
			this.VisitErrorNode(new ErrorNode(Node, "no constructor: " + Node.Type));
		}
	}

	protected void VisitVarDeclNode(BunLetVarNode Node) {
		Class<?> DeclClass = this.GetJavaClass(Node.DeclType());
		this.AsmBuilder.addLocal(DeclClass, Node.GetGivenName());
		this.AsmBuilder.pushNode(DeclClass, Node.InitValueNode());
		this.AsmBuilder.storeLocal(Node.GetGivenName());
	}

	protected void VisitVarDeclNode2(BunLetVarNode Node) {
		Class<?> DeclClass = this.GetJavaClass(Node.DeclType());
		this.AsmBuilder.removeLocal(DeclClass, Node.GetGivenName());
	}

	@Override public void VisitVarBlockNode(BunVarBlockNode Node) {
		this.VisitVarDeclNode(Node.VarDeclNode());
		this.VisitBlockNode(Node);
		this.VisitVarDeclNode2(Node.VarDeclNode());
	}

	public void VisitStaticFieldNode(JavaStaticFieldNode Node) {
		this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(Node.StaticClass), Node.FieldName, this.GetJavaClass(Node.Type));
	}

	protected void VisitGlobalNameNode(GetNameNode Node) {
		if(Node.ResolvedNode instanceof BunLetVarNode) {
			BunLetVarNode LetNode = Node.ResolvedNode;
			Class<?> JavaClass = this.GetJavaClass(LetNode.GetAstType(BunLetVarNode._NameInfo));
			this.AsmBuilder.visitFieldInsn(GETSTATIC, this.NameGlobalNameClass(LetNode.GetUniqueName(this)), "_", JavaClass);
		}
		else {
			this.VisitErrorNode(new ErrorNode(Node, "unimplemented ResolvedNode: " + Node.ResolvedNode.getClass().getName()));
		}
	}

	@Override public void VisitGetNameNode(GetNameNode Node) {
		if(Node.ResolvedNode == null) {
			this.VisitErrorNode(new ErrorNode(Node, "undefined symbol: " + Node.GivenName));
			return;
		}
		if(Node.ResolvedNode.GetDefiningFunctionNode() == null) {
			this.VisitGlobalNameNode(Node);
			return;
		}
		this.AsmBuilder.loadLocal(Node.GetUniqueName(this));
		this.AsmBuilder.checkReturnCast(Node, this.AsmBuilder.getLocalType(Node.GetUniqueName(this)));
	}

	protected void GenerateAssignNode(GetNameNode Node, BNode ExprNode) {
		@Var String Name = Node.GetUniqueName(this);
		this.AsmBuilder.pushNode(this.AsmBuilder.getLocalType(Name), ExprNode);
		this.AsmBuilder.storeLocal(Name);
	}


	@Override public void VisitAssignNode(AssignNode Node) {
		@Var BNode LeftNode = Node.LeftNode();
		if(LeftNode instanceof GetNameNode) {
			this.GenerateAssignNode((GetNameNode)LeftNode, Node.RightNode());
		}
		else if(LeftNode instanceof GetFieldNode) {
			this.GenerateAssignNode((GetFieldNode)LeftNode, Node.RightNode());
		}
		else if(LeftNode instanceof GetIndexNode) {
			this.GenerateAssignNode((GetIndexNode)LeftNode, Node.RightNode());
		}
	}

	@Override public void VisitGroupNode(GroupNode Node) {
		Node.ExprNode().Accept(this);
	}

	private Field GetField(Class<?> RecvClass, String Name) {
		try {
			return RecvClass.getField(Name);
		} catch (Exception e) {
			LibBunSystem._FixMe(e);
		}
		return null;  // type checker guarantees field exists
	}

	@Override public void VisitGetFieldNode(GetFieldNode Node) {
		assert !Node.IsUntyped();
		Class<?> RecvClass = this.GetJavaClass(Node.RecvNode().Type);
		Field jField = this.GetField(RecvClass, Node.GetName());
		String Owner = Type.getType(RecvClass).getInternalName();
		String Desc = Type.getType(jField.getType()).getDescriptor();
		if(Modifier.isStatic(jField.getModifiers())) {
			this.AsmBuilder.visitFieldInsn(Opcodes.GETSTATIC, Owner, Node.GetName(), Desc);
		}
		else {
			this.AsmBuilder.pushNode(null, Node.RecvNode());
			this.AsmBuilder.visitFieldInsn(GETFIELD, Owner, Node.GetName(), Desc);
		}
		this.AsmBuilder.checkReturnCast(Node, jField.getType());
	}

	protected void GenerateAssignNode(GetFieldNode Node, BNode ExprNode) {
		assert !Node.IsUntyped();
		Class<?> RecvClass = this.GetJavaClass(Node.RecvNode().Type);
		Field jField = this.GetField(RecvClass, Node.GetName());
		String Owner = Type.getType(RecvClass).getInternalName();
		String Desc = Type.getType(jField.getType()).getDescriptor();
		if(Modifier.isStatic(jField.getModifiers())) {
			this.AsmBuilder.pushNode(jField.getType(), ExprNode);
			this.AsmBuilder.visitFieldInsn(PUTSTATIC, Owner, Node.GetName(), Desc);
		}
		else {
			this.AsmBuilder.pushNode(null, Node.RecvNode());
			this.AsmBuilder.pushNode(jField.getType(), ExprNode);
			this.AsmBuilder.visitFieldInsn(PUTFIELD, Owner, Node.GetName(), Desc);
		}
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		Method sMethod = this.methodTable.GetBinaryStaticMethod(Node.RecvNode().Type, "[]", Node.IndexNode().Type);
		this.AsmBuilder.applyStaticMethod(Node, sMethod, new BNode[] {Node.RecvNode(), Node.IndexNode()});
	}

	protected void GenerateAssignNode(GetIndexNode Node, BNode ExprNode) {
		Method sMethod = this.methodTable.GetBinaryStaticMethod(Node.RecvNode().Type, "[]=", Node.IndexNode().Type);
		this.AsmBuilder.applyStaticMethod(Node.ParentNode, sMethod, new BNode[] {Node.RecvNode(), Node.IndexNode(), ExprNode});
	}

	private int GetInvokeType(Method jMethod) {
		if(Modifier.isStatic(jMethod.getModifiers())) {
			return INVOKESTATIC;
		}
		if(Modifier.isInterface(jMethod.getModifiers())) {
			return INVOKEINTERFACE;
		}
		return INVOKEVIRTUAL;
	}

	@Override public void VisitMethodCallNode(MethodCallNode Node) {
		this.AsmBuilder.setLineNumber(Node);
		Method jMethod = this.javaTypeUtils.GetMethod(Node.RecvNode().Type, Node.MethodName(), Node);
		assert jMethod != null;
		if(!Modifier.isStatic(jMethod.getModifiers())) {
			this.AsmBuilder.pushNode(null, Node.RecvNode());
		}
		Class<?>[] P = jMethod.getParameterTypes();
		for(int i = 0; i < P.length; i++) {
			this.AsmBuilder.pushNode(P[i], Node.GetListAt(i));
		}
		int inst = this.GetInvokeType(jMethod);
		String owner = Type.getInternalName(jMethod.getDeclaringClass());
		this.AsmBuilder.visitMethodInsn(inst, owner, jMethod.getName(), Type.getMethodDescriptor(jMethod));
		this.AsmBuilder.checkReturnCast(Node, jMethod.getReturnType());
	}

	@Override public void VisitFormNode(BunFormNode Node) {
		for(int i = 0; i < Node.GetListSize(); i++) {
			this.AsmBuilder.pushNode(null, Node.GetListAt(i));
		}
		@Var String FormText = Node.FormFunc.FormText;
		@Var int ClassEnd = FormText.indexOf(".");
		@Var int MethodEnd = FormText.indexOf("(");
		//System.out.println("FormText: " + FormText + " " + ClassEnd + ", " + MethodEnd);
		@Var String ClassName = FormText.substring(0, ClassEnd);
		@Var String MethodName = FormText.substring(ClassEnd+1, MethodEnd);
		this.AsmBuilder.setLineNumber(Node);
		//System.out.println("debug: " + ClassName + ", " + MethodName);
		this.AsmBuilder.visitMethodInsn(INVOKESTATIC, ClassName, MethodName, Node.FormFunc.FuncType);
	}

	@Override public void VisitFuncCallNode(FuncCallNode Node) {
		@Var BType FuncType = Node.FunctorNode().Type;
		@Var BunFuncNameNode FuncNameNode = Node.FuncNameNode();
		if(!(FuncType instanceof BFuncType)) { // lookup func type
			@Var BFunc Func = ((BunTypeSafer)this.TypeChecker).LookupFunc(Node.GetGamma(), FuncNameNode.FuncName, FuncNameNode.RecvType, FuncNameNode.FuncParamSize);
			if(Func != null) {
				FuncType = Func.GetFuncType();
				Node.Type = ((BFuncType)FuncType).GetReturnType();
			}
		}
		if(FuncType instanceof BFuncType) {
			if(FuncNameNode != null) {
				this.AsmBuilder.applyFuncName(FuncNameNode, FuncNameNode.FuncName, (BFuncType)FuncType, Node);
			}
			else {
				Class<?> FuncClass = this.LoadFuncClass((BFuncType)FuncType);
				this.AsmBuilder.applyFuncObject(Node, FuncClass, Node.FunctorNode(), (BFuncType)FuncType, Node);
			}
		}
		else {
			this.VisitErrorNode(new ErrorNode(Node, "not function"));
		}
	}

	@Override public void VisitUnaryNode(UnaryOperatorNode Node) {
		Method sMethod = this.methodTable.GetUnaryStaticMethod(Node.SourceToken.GetText(), Node.RecvNode().Type);
		this.AsmBuilder.applyStaticMethod(Node, sMethod, new BNode[] {Node.RecvNode()});
	}

	@Override public void VisitNotNode(BunNotNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitPlusNode(BunPlusNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitMinusNode(BunMinusNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitComplementNode(BunComplementNode Node) {
		this.VisitUnaryNode(Node);
	}

	@Override public void VisitCastNode(BunCastNode Node) {
		if(Node.Type.IsVoidType()) {
			Node.ExprNode().Accept(this);
			this.AsmBuilder.Pop(Node.ExprNode().Type);
		}
		else {
			Class<?> TargetClass = this.GetJavaClass(Node.Type);
			Class<?> SourceClass = this.GetJavaClass(Node.ExprNode().Type);
			Method sMethod = this.methodTable.GetCastMethod(TargetClass, SourceClass);
			if(sMethod != null) {
				this.AsmBuilder.applyStaticMethod(Node, sMethod, new BNode[] {Node.ExprNode()});
			}
			else if(!TargetClass.isAssignableFrom(SourceClass)) {
				this.AsmBuilder.visitTypeInsn(CHECKCAST, TargetClass);
			}
		}
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode Node) {
		if(!(Node.TargetType() instanceof BClassType) || !(Node.LeftNode().Type instanceof BClassType)) {
			this.VisitErrorNode(new ErrorNode(Node, "require Class Type"));
			return;
		}
		Class<?> JavaClass = this.GetJavaClass(Node.TargetType());
		Node.LeftNode().Accept(this);
		this.AsmBuilder.visitTypeInsn(INSTANCEOF, JavaClass);
	}

	@Override public void VisitBinaryNode(BinaryOperatorNode Node) {
		Method sMethod = this.methodTable.GetBinaryStaticMethod(Node.LeftNode().Type, Node.GetOperator(), Node.RightNode().Type);
		this.AsmBuilder.applyStaticMethod(Node, sMethod, new BNode[] {Node.LeftNode(), Node.RightNode()});
	}

	@Override public void VisitAddNode(BunAddNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitSubNode(BunSubNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitMulNode(BunMulNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitDivNode(BunDivNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitModNode(BunModNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitLeftShiftNode(BunLeftShiftNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitBitwiseAndNode(BunBitwiseAndNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitBitwiseOrNode(BunBitwiseOrNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitBitwiseXorNode(BunBitwiseXorNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitEqualsNode(BunEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitNotEqualsNode(BunNotEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitLessThanNode(BunLessThanNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitLessThanEqualsNode(BunLessThanEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitGreaterThanNode(BunGreaterThanNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode Node) {
		this.VisitBinaryNode(Node);
	}

	@Override public void VisitAndNode(BunAndNode Node) {
		Label elseLabel = new Label();
		Label mergeLabel = new Label();
		this.AsmBuilder.pushNode(boolean.class, Node.LeftNode());
		this.AsmBuilder.visitJumpInsn(IFEQ, elseLabel);

		this.AsmBuilder.pushNode(boolean.class, Node.RightNode());
		this.AsmBuilder.visitJumpInsn(IFEQ, elseLabel);

		this.AsmBuilder.visitLdcInsn(true);
		this.AsmBuilder.visitJumpInsn(GOTO, mergeLabel);

		this.AsmBuilder.visitLabel(elseLabel);
		this.AsmBuilder.visitLdcInsn(false);
		this.AsmBuilder.visitJumpInsn(GOTO, mergeLabel);

		this.AsmBuilder.visitLabel(mergeLabel);
	}

	@Override public void VisitOrNode(BunOrNode Node) {
		Label thenLabel = new Label();
		Label mergeLabel = new Label();
		this.AsmBuilder.pushNode(boolean.class, Node.LeftNode());
		this.AsmBuilder.visitJumpInsn(IFNE, thenLabel);

		this.AsmBuilder.pushNode(boolean.class, Node.RightNode());
		this.AsmBuilder.visitJumpInsn(IFNE, thenLabel);

		this.AsmBuilder.visitLdcInsn(false);
		this.AsmBuilder.visitJumpInsn(GOTO, mergeLabel);

		this.AsmBuilder.visitLabel(thenLabel);
		this.AsmBuilder.visitLdcInsn(true);
		this.AsmBuilder.visitJumpInsn(GOTO, mergeLabel);

		this.AsmBuilder.visitLabel(mergeLabel);
	}

	@Override public void VisitBlockNode(BunBlockNode Node) {
		for (int i = 0; i < Node.GetListSize(); i++) {
			Node.GetListAt(i).Accept(this);
		}
	}

	@Override public void VisitIfNode(BunIfNode Node) {
		Label ElseLabel = new Label();
		Label EndLabel = new Label();
		this.AsmBuilder.pushNode(boolean.class, Node.CondNode());
		this.AsmBuilder.visitJumpInsn(IFEQ, ElseLabel);
		// Then
		Node.ThenNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(GOTO, EndLabel);
		// Else
		this.AsmBuilder.visitLabel(ElseLabel);
		if(Node.ElseNode() != null) {
			Node.ElseNode().Accept(this);
			this.AsmBuilder.visitJumpInsn(GOTO, EndLabel);
		}
		// End
		this.AsmBuilder.visitLabel(EndLabel);
	}

	@Override public void VisitReturnNode(BunReturnNode Node) {
		if(Node.HasReturnExpr()) {
			Node.ExprNode().Accept(this);
			Type type = this.javaTypeUtils.AsmType(Node.ExprNode().Type);
			this.AsmBuilder.visitInsn(type.getOpcode(IRETURN));
		}
		else {
			this.AsmBuilder.visitInsn(RETURN);
		}
	}

	@Override public void VisitWhileNode(BunWhileNode Node) {
		if(Node.HasNextNode()) {
			Node.BlockNode().Append(Node.NextNode());
		}
		Label continueLabel = new Label();
		Label breakLabel = new Label();
		this.AsmBuilder.breakLabelStack.push(breakLabel);
		this.AsmBuilder.continueLabelStack.push(continueLabel);

		this.AsmBuilder.visitLabel(continueLabel);
		this.AsmBuilder.pushNode(boolean.class, Node.CondNode());
		this.AsmBuilder.visitJumpInsn(IFEQ, breakLabel); // condition
		Node.BlockNode().Accept(this);
		this.AsmBuilder.visitJumpInsn(GOTO, continueLabel);
		this.AsmBuilder.visitLabel(breakLabel);

		this.AsmBuilder.breakLabelStack.pop();
		this.AsmBuilder.continueLabelStack.pop();
	}

	@Override public void VisitBreakNode(BunBreakNode Node) {
		Label l = this.AsmBuilder.breakLabelStack.peek();
		this.AsmBuilder.visitJumpInsn(GOTO, l);
	}

	@Override public void VisitThrowNode(BunThrowNode Node) { //TODO: exception wrapper
		String ClassName = Type.getInternalName(SoftwareFault.class);
		this.AsmBuilder.setLineNumber(Node);
		this.AsmBuilder.visitTypeInsn(NEW, ClassName);
		this.AsmBuilder.visitInsn(DUP);
		this.AsmBuilder.pushNode(Object.class, Node.ExprNode());
		String Desc = Type.getMethodDescriptor(Type.getType(void.class), new Type[] { Type.getType(Object.class)});
		this.AsmBuilder.visitMethodInsn(INVOKESPECIAL, ClassName, "<init>", Desc);
		this.AsmBuilder.visitInsn(ATHROW);
	}

	@Override public void VisitTryNode(BunTryNode Node) {	//do nothing
	}

	@Override public void VisitLetNode(BunLetVarNode Node) {	//TODO
	}

	Class<?> LoadFuncClass(BFuncType FuncType) {
		String ClassName = this.NameType(FuncType);
		Class<?> FuncClass = this.GetGeneratedClass(ClassName, null);
		if(FuncClass == null) {
			@Var String SuperClassName = Type.getInternalName(BFunction.class);
			@Var ClassBuilder ClassBuilder = this.AsmLoader.newClassBuilder(ACC_PUBLIC| ACC_ABSTRACT, null, ClassName, BFunction.class);
			MethodBuilder InvokeMethod = ClassBuilder.newMethodBuilder(ACC_PUBLIC | ACC_ABSTRACT, "Invoke", FuncType);
			InvokeMethod.finish();

			MethodBuilder InitMethod = ClassBuilder.newMethodBuilder(ACC_PUBLIC, "<init>", "(ILjava/lang/String;)V");
			InitMethod.visitVarInsn(ALOAD, 0);
			InitMethod.visitVarInsn(ILOAD, 1);
			InitMethod.visitVarInsn(ALOAD, 2);
			InitMethod.visitMethodInsn(INVOKESPECIAL, SuperClassName, "<init>", "(ILjava/lang/String;)V");
			InitMethod.visitInsn(RETURN);
			InitMethod.finish();

			FuncClass = this.AsmLoader.loadGeneratedClass(ClassName);
			this.SetGeneratedClass(ClassName, FuncClass);
		}
		return FuncClass;
	}

	@Override public void VisitFunctionNode(BunFunctionNode Node) {
		if(Node.IsTopLevelDefineFunction()) {
			assert(Node.FuncName() != null);
			assert(Node.IsTopLevel());  // otherwise, transformed to var f = function ()..
			JavaStaticFieldNode FuncNode = this.GenerateFunctionAsSymbolField(Node.FuncName(), Node);
			if(Node.IsExport) {
				if(Node.FuncName().equals("main")) {
					this.MainFuncNode = FuncNode;
				}
			}
			this.SetMethod(Node.FuncName(), (BFuncType)FuncNode.Type, FuncNode.StaticClass);
		}
		else {
			JavaStaticFieldNode FuncNode = this.GenerateFunctionAsSymbolField(Node.GetUniqueName(this), Node);
			if(this.AsmBuilder != null) {
				this.VisitStaticFieldNode(FuncNode);
			}
		}
	}

	private JavaStaticFieldNode GenerateFunctionAsSymbolField(String FuncName, BunFunctionNode Node) {
		@Var BFuncType FuncType = Node.GetFuncType();
		String ClassName = this.NameFunctionClass(FuncName, FuncType);
		Class<?> FuncClass = this.LoadFuncClass(FuncType);
		@Var ClassBuilder ClassBuilder = this.AsmLoader.newClassBuilder(ACC_PUBLIC|ACC_FINAL, Node, ClassName, FuncClass);

		MethodBuilder InvokeMethod = ClassBuilder.newMethodBuilder(ACC_PUBLIC | ACC_FINAL, "Invoke", FuncType);
		int index = 1;
		for(int i = 0; i < FuncType.GetFuncParamSize(); i++) {
			Type AsmType = this.javaTypeUtils.AsmType(FuncType.GetFuncParamType(i));
			InvokeMethod.visitVarInsn(AsmType.getOpcode(ILOAD), index);
			index += AsmType.getSize();
		}
		InvokeMethod.visitMethodInsn(INVOKESTATIC, ClassName, "f", FuncType);
		InvokeMethod.visitReturn(FuncType.GetReturnType());
		InvokeMethod.finish();

		ClassBuilder.addField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "function", FuncClass, null);

		// static init
		MethodBuilder StaticInitMethod = ClassBuilder.newMethodBuilder(ACC_PUBLIC | ACC_STATIC , "<clinit>", "()V");
		StaticInitMethod.visitTypeInsn(NEW, ClassName);
		StaticInitMethod.visitInsn(DUP);
		StaticInitMethod.visitMethodInsn(INVOKESPECIAL, ClassName, "<init>", "()V");
		StaticInitMethod.visitFieldInsn(PUTSTATIC, ClassName, "function",  FuncClass);
		StaticInitMethod.visitInsn(RETURN);
		StaticInitMethod.finish();

		MethodBuilder InitMethod = ClassBuilder.newMethodBuilder(ACC_PRIVATE, "<init>", "()V");
		InitMethod.visitVarInsn(ALOAD, 0);
		InitMethod.visitLdcInsn(FuncType.TypeId);
		InitMethod.visitLdcInsn(FuncName);
		InitMethod.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(FuncClass), "<init>", "(ILjava/lang/String;)V");
		InitMethod.visitInsn(RETURN);
		InitMethod.finish();

		MethodBuilder StaticFuncMethod = ClassBuilder.newMethodBuilder(ACC_PUBLIC | ACC_STATIC, "f", FuncType);
		for(int i = 0; i < Node.GetListSize(); i++) {
			BunLetVarNode ParamNode = Node.GetParamNode(i);
			Class<?> DeclClass = this.GetJavaClass(ParamNode.DeclType());
			StaticFuncMethod.addLocal(DeclClass, ParamNode.GetGivenName());
		}
		Node.BlockNode().Accept(this);
		StaticFuncMethod.finish();

		FuncClass = this.AsmLoader.loadGeneratedClass(ClassName);
		this.SetGeneratedClass(ClassName, FuncClass);
		return new JavaStaticFieldNode(null, FuncClass, FuncType, "function");
	}

	private BFunction LoadFunction(Class<?> WrapperClass, Class<?> StaticMethodClass) {
		try {
			Field f = StaticMethodClass.getField("function");
			Object func = f.get(null);
			if(WrapperClass != null) {
				Constructor<?> c = WrapperClass.getConstructor(func.getClass().getSuperclass());
				func = c.newInstance(func);
			}
			return (BFunction)func;
		}
		catch(Exception e) {
			e.printStackTrace();
			LibBunSystem._Exit(1, "failed: " + e);
		}
		return null;
	}

	private void SetMethod(String FuncName, BFuncType FuncType, Class<?> FuncClass) {
		BType RecvType = FuncType.GetRecvType();
		if(RecvType instanceof BClassType && FuncName != null) {
			BClassType ClassType = (BClassType)RecvType;
			BType FieldType = ClassType.GetFieldType(FuncName, null);
			if(FieldType == null || !FieldType.IsFuncType()) {
				FuncName = LibBunSystem._AnotherName(FuncName);
				FieldType = ClassType.GetFieldType(FuncName, null);
				if(FieldType == null || !FieldType.IsFuncType()) {
					return;
				}
			}
			if(FieldType.Equals(FuncType)) {
				this.SetMethod(ClassType, FuncName, this.LoadFunction(null, FuncClass));
			}
			else if(this.IsMethodFuncType((BFuncType)FieldType, FuncType)) {
				Class<?> WrapperClass = this.MethodWrapperClass((BFuncType)FieldType, FuncType);
				this.SetMethod(ClassType, FuncName, this.LoadFunction(WrapperClass, FuncClass));
			}
		}
	}

	private boolean IsMethodFuncType(BFuncType FieldType, BFuncType FuncType) {
		if(FuncType.GetFuncParamSize() == FieldType.GetFuncParamSize() && FuncType.GetReturnType().Equals(FieldType.GetReturnType())) {
			for(int i = 1; i < FuncType.GetFuncParamSize(); i++) {
				if(!FuncType.GetFuncParamType(i).Equals(FieldType.GetFuncParamType(i))) {
					return false;
				}
			}
		}
		return true;
	}

	private Class<?> MethodWrapperClass(BFuncType FuncType, BFuncType SourceFuncType) {
		String ClassName = "W" + this.NameType(FuncType) + "W" + this.NameType(SourceFuncType);
		Class<?> WrapperClass = this.GetGeneratedClass(ClassName, null);
		if(WrapperClass == null) {
			Class<?> FuncClass = this.LoadFuncClass(FuncType);
			Class<?> SourceClass = this.LoadFuncClass(SourceFuncType);
			@Var ClassBuilder ClassBuilder = this.AsmLoader.newClassBuilder(ACC_PUBLIC|ACC_FINAL, null, ClassName, FuncClass);

			ClassBuilder.addField(ACC_PUBLIC, "f", SourceClass, null);

			MethodBuilder InitMethod = ClassBuilder.newMethodBuilder(ACC_PUBLIC, "<init>", "(L"+Type.getInternalName(SourceClass)+";)V");
			InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
			InitMethod.pushInt(FuncType.TypeId);
			InitMethod.visitLdcInsn(SourceFuncType.GetName());
			InitMethod.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(FuncClass), "<init>", "(ILjava/lang/String;)V");
			InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
			InitMethod.visitVarInsn(Opcodes.ALOAD, 1);
			InitMethod.visitFieldInsn(PUTFIELD, ClassName, "f", Type.getDescriptor(SourceClass));
			InitMethod.visitInsn(RETURN);
			InitMethod.finish();

			MethodBuilder InvokeMethod = ClassBuilder.newMethodBuilder(ACC_PUBLIC | ACC_FINAL, "Invoke", FuncType);
			InvokeMethod.visitVarInsn(ALOAD, 0);
			InvokeMethod.visitFieldInsn(GETFIELD, ClassName, "f", Type.getDescriptor(SourceClass));
			InvokeMethod.visitVarInsn(ALOAD, 1);
			//			System.out.println("CAST: " + Type.getInternalName(this.GetJavaClass(SourceFuncType.GetFuncParamType(0))));
			InvokeMethod.visitTypeInsn(CHECKCAST, this.GetJavaClass(SourceFuncType.GetFuncParamType(0)));
			int index = 2;
			for(int i = 1; i < FuncType.GetFuncParamSize(); i++) {
				Type AsmType = this.javaTypeUtils.AsmType(FuncType.GetFuncParamType(i));
				InvokeMethod.visitVarInsn(AsmType.getOpcode(ILOAD), index);
				index += AsmType.getSize();
			}
			//String owner = "C" + FuncType.StringfySignature(FuncName);
			InvokeMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(SourceClass), "Invoke", this.javaTypeUtils.GetMethodDescriptor(SourceFuncType));
			InvokeMethod.visitReturn(FuncType.GetReturnType());
			InvokeMethod.finish();

			WrapperClass = this.AsmLoader.loadGeneratedClass(ClassName);
			this.SetGeneratedClass(ClassName, WrapperClass);
		}
		return WrapperClass;
	}


	// -----------------------------------------------------------------------

	private Class<?> GetSuperClass(BType SuperType) {
		@Var Class<?> SuperClass = null;
		if(SuperType != null) {
			SuperClass = this.GetJavaClass(SuperType);
		}
		else {
			SuperClass = BunObject.class;
		}
		return SuperClass;
	}

	private final static String NameClassMethod(BType ClassType, String FieldName) {
		return FieldName + ClassType.TypeId;
	}

	private void SetMethod(BClassType ClassType, String FuncName, BFunction FuncObject) {
		try {
			Class<?> StaticClass = this.GetJavaClass(ClassType);
			Field f = StaticClass.getField(NameClassMethod(ClassType, FuncName));
			f.set(null, FuncObject);
		}
		catch (Exception e) {
			e.printStackTrace();
			LibBunSystem._Exit(1, "failed " + e);
		}
	}

	private Object GetConstValue(BNode Node) {
		if(Node instanceof BunNullNode) {
			return null;
		}
		if(Node instanceof BunBooleanNode) {
			return ((BunBooleanNode)Node).BooleanValue;
		}
		if(Node instanceof BunIntNode) {
			return ((BunIntNode)Node).IntValue;
		}
		if(Node instanceof BunFloatNode) {
			return ((BunFloatNode)Node).FloatValue;
		}
		if(Node instanceof BunStringNode) {
			return ((BunStringNode)Node).StringValue;
		}
		if(Node instanceof BunTypeNode) {
			return Node.Type;
		}
		return null;
	}

	@Override public void VisitClassNode(BunClassNode Node) {
		@Var Class<?> SuperClass = this.GetSuperClass(Node.SuperType());
		@Var ClassBuilder ClassBuilder = this.AsmLoader.newClassBuilder(ACC_PUBLIC, Node, Node.ClassName(), SuperClass);
		// add class field
		for(int i = 0; i < Node.GetListSize(); i++) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			ClassBuilder.addField(ACC_PUBLIC, FieldNode.GetGivenName(), FieldNode.DeclType(), this.GetConstValue(FieldNode.InitValueNode()));
		}
		// add static field (only function)
		for(int i = 0; i < Node.ClassType.GetFieldSize(); i++) {
			@Var BClassField Field = Node.ClassType.GetFieldAt(i);
			if(Field.FieldType.IsFuncType()) {
				ClassBuilder.addField(ACC_PUBLIC|ACC_STATIC, NameClassMethod(Node.ClassType, Field.FieldName), Field.FieldType, null);
			}
		}
		// public <init>()
		MethodBuilder InitMethod = ClassBuilder.newMethodBuilder(ACC_PUBLIC, "<init>", "()V");
		InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
		InitMethod.pushInt(Node.ClassType.TypeId);
		InitMethod.visitMethodInsn(INVOKESPECIAL, Node.ClassName(), "<init>", "(I)V");
		InitMethod.visitInsn(RETURN);
		InitMethod.finish();
		// protected <init>(int typeid)
		InitMethod = ClassBuilder.newMethodBuilder(ACC_PROTECTED, "<init>", "(I)V");
		InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
		//		InitMethod.visitVarInsn(Opcodes.ILOAD, 1);
		//		InitMethod.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(SuperClass), "<init>", "(I)V");
		InitMethod.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(SuperClass), "<init>", "()V");	// FIXME: ZObject?
		for(int i = 0; i < Node.GetListSize(); i++) {
			@Var BunLetVarNode FieldNode = Node.GetFieldNode(i);
			if(!FieldNode.DeclType().IsFuncType()) {
				InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
				InitMethod.pushNode(this.GetJavaClass(FieldNode.DeclType()), FieldNode.InitValueNode());
				InitMethod.visitFieldInsn(PUTFIELD, Node.ClassName(), FieldNode.GetGivenName(), Type.getDescriptor(this.GetJavaClass(FieldNode.DeclType())));
			}
		}
		// set function
		for(int i = 0; i < Node.ClassType.GetFieldSize(); i++) {
			@Var BClassField Field = Node.ClassType.GetFieldAt(i);
			if(Field.FieldType.IsFuncType()) {
				String FieldDesc = Type.getDescriptor(this.GetJavaClass(Field.FieldType));
				Label JumpLabel = new Label();
				InitMethod.visitFieldInsn(Opcodes.GETSTATIC, Node.ClassName(), NameClassMethod(Node.ClassType, Field.FieldName), FieldDesc);
				InitMethod.visitJumpInsn(Opcodes.IFNULL, JumpLabel);
				InitMethod.visitVarInsn(Opcodes.ALOAD, 0);
				InitMethod.visitFieldInsn(Opcodes.GETSTATIC, Node.ClassName(), NameClassMethod(Node.ClassType, Field.FieldName), FieldDesc);
				InitMethod.visitFieldInsn(Opcodes.PUTFIELD, Node.ClassName(), Field.FieldName, FieldDesc);
				InitMethod.visitLabel(JumpLabel);
			}
		}

		InitMethod.visitInsn(RETURN);
		InitMethod.finish();

		this.typeTable.SetTypeTable(Node.ClassType, this.AsmLoader.loadGeneratedClass(Node.ClassName()));
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
		//TODO
	}

	@Override public void VisitAsmNode(BunAsmNode Node) {
		// TODO Auto-generated method stub
	}

	@Override public void VisitTopLevelNode(TopLevelNode Node) {
		this.VisitUndefinedNode(Node);
	}

	@Override public void VisitLocalDefinedNode(LocalDefinedNode Node) {
		if(Node instanceof JavaStaticFieldNode) {
			this.VisitStaticFieldNode(((JavaStaticFieldNode)Node));
		}
		else {
			this.VisitUndefinedNode(Node);
		}
	}

	public final void Debug(String Message) {
		LibBunSystem._PrintDebug(Message);
	}

	@Override public void VisitLiteralNode(LiteralNode Node) {
	}

	JavaMethodTable getMethodTable() {
		return this.methodTable;
	}
}
