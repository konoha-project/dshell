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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.GroupNode;
import libbun.ast.LocalDefinedNode;
import libbun.ast.SyntaxSugarNode;
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
import libbun.ast.sugar.BunContinueNode;
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
import libbun.type.BFormFunc;
import libbun.type.BFunc;
import libbun.type.BFuncType;
import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.util.BArray;
import libbun.util.BFunction;
import libbun.util.BMatchFunction;
import libbun.util.BTokenFunction;
import libbun.util.BunMap;
import libbun.util.BunObject;
import libbun.util.LibBunSystem;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import dshell.internal.ast.CommandNode;
import dshell.internal.ast.DShellCatchNode;
import dshell.internal.ast.DShellForNode;
import dshell.internal.ast.DShellTryNode;
import dshell.internal.ast.DShellWrapperNode;
import dshell.internal.ast.InternalFuncCallNode;
import dshell.internal.ast.MatchRegexNode;
import dshell.internal.lang.DShellVisitor;
import dshell.internal.lib.CommandArg;
import dshell.internal.lib.GlobalVariableTable;
import dshell.internal.lib.StreamUtils;
import dshell.internal.lib.TaskBuilder;
import dshell.internal.lib.Utils;
import dshell.internal.lib.CommandArg.SubstitutedArg;
import dshell.lang.DShellException;
import dshell.lang.Errno;
import dshell.lang.MultipleException;
import dshell.lang.NativeException;
import dshell.lang.Task;

public class JavaByteCodeGenerator extends LibBunGenerator implements DShellVisitor {
	protected final JavaTypeTable typeTable;
	protected final JavaMethodTable methodTable;
	public final JavaTypeUtils javaTypeUtils;
	protected final HashMap<String, Class<?>> generatedClassMap;
	protected final HashMap<String, BNode> lazyNodeMap;
	public JavaStaticFieldNode mainFuncNode = null;
	protected GeneratedClassLoader asmLoader = null;
	protected MethodBuilder methodBuilder;

	private Method execCommandVoid;
	private Method execCommandBool;
	private Method execCommandInt;
	private Method execCommandString;
	private Method execCommandStringArray;
	private Method execCommandTask;
	private Method execCommandTaskArray;

	private Method wrapException;

	public JavaByteCodeGenerator() {
		super(new LibBunLangInfo("Java1.6", "jvm"));
		this.typeTable = new JavaTypeTable();
		this.methodTable = new JavaMethodTable(this.typeTable);
		this.generatedClassMap = new HashMap<String, Class<?>>();
		this.lazyNodeMap = new HashMap<String, BNode>();
		this.javaTypeUtils = new JavaTypeUtils(this, this.typeTable);
		this.initFuncClass();
		//this.ImportLocalGrammar(this.RootGamma);
		this.asmLoader = new GeneratedClassLoader(this);

		//
		this.loadJavaClass(Task.class);
		this.loadJavaClass(dshell.lang.Exception.class);
		this.loadJavaClass(DShellException.class);
		this.loadJavaClass(MultipleException.class);
		this.loadJavaClass(Errno.UnimplementedErrnoException.class);
		this.loadJavaClass(DShellException.NullException.class);
		this.loadJavaClass(NativeException.class);
		this.loadJavaClassList(Errno.getExceptionClassList());
		this.loadJavaClass(CommandArg.class);
		this.loadJavaClass(SubstitutedArg.class);
		this.loadJavaClass(StreamUtils.InputStream.class);
		this.loadJavaClass(StreamUtils.OutputStream.class);

		try {
			this.execCommandVoid = TaskBuilder.class.getMethod("execCommandVoid", CommandArg[][].class);
			this.execCommandBool = TaskBuilder.class.getMethod("execCommandBool", CommandArg[][].class);
			this.execCommandInt = TaskBuilder.class.getMethod("execCommandInt", CommandArg[][].class);
			this.execCommandString = TaskBuilder.class.getMethod("execCommandString", CommandArg[][].class);
			this.execCommandStringArray = TaskBuilder.class.getMethod("execCommandStringArray", CommandArg[][].class);
			this.execCommandTask = TaskBuilder.class.getMethod("execCommandTask", CommandArg[][].class);
			this.execCommandTaskArray = TaskBuilder.class.getMethod("execCommandTaskArray", CommandArg[][].class);

			this.wrapException = NativeException.class.getMethod("wrapException", Throwable.class);
		}
		catch(Throwable e) {
			e.printStackTrace();
			Utils.fatal(1, "method loading failed");
		}
		this.methodTable.Import(BType.StringType, "=~", BType.StringType, Utils.class, "matchRegex");
		this.methodTable.Import(BType.StringType, "!~", BType.StringType, Utils.class, "unmatchRegex");

		// load static method
		this.loadJavaStaticMethod(Utils.class, "getEnv", String.class);
		this.loadJavaStaticMethod(Utils.class, "setEnv", String.class, String.class);
		this.loadJavaStaticMethod(CommandArg.class, "createCommandArg", String.class);
		this.loadJavaStaticMethod(CommandArg.class, "createSubstitutedArg", String.class);
		this.loadJavaStaticMethod(Utils.class, "assertDShell", boolean.class);
		this.loadJavaStaticMethod(JavaCommonApi.class, "_", "ObjectToString", Object.class);
		this.loadJavaStaticMethod(Utils.class, "_", "stringToLong", String.class);
		this.loadJavaStaticMethod(Utils.class, "_", "stringToDouble", String.class);
	}

//	private void ImportLocalGrammar(LibBunGamma Gamma) {
//		Gamma.DefineStatement("import", new JavaImportPattern());
//		Gamma.DefineExpression("$JavaClassPath$", new JavaClassPathPattern());
//	}

	private void initFuncClass() {
		BFuncType funcType = this.typeTable.FuncType(boolean.class, BSourceContext.class);
		this.setGeneratedClass(this.NameType(funcType), BTokenFunction.class);
		funcType = this.typeTable.FuncType(BNode.class, BNode.class, BTokenContext.class, BNode.class);
		this.setGeneratedClass(this.NameType(funcType), BMatchFunction.class);
	}

	private final void setGeneratedClass(String key, Class<?> generatedClass) {
		this.generatedClassMap.put(key, generatedClass);
	}

	private final Class<?> getGeneratedClass(String key, Class<?> defaultClass) {
		Class<?> C = this.generatedClassMap.get(key);
		if(C != null) {
			return C;
		}
		return defaultClass;
	}

	public Class<?> getDefinedFunctionClass(String funcName, BFuncType funcType) {
		return this.generatedClassMap.get(this.NameFunctionClass(funcName, funcType));
	}

	public Class<?> getDefinedFunctionClass(String funcName, BType recvType, int funcParamSize) {
		return this.generatedClassMap.get(this.NameFunctionClass(funcName, recvType, funcParamSize));
	}

	protected void lazyBuild(BunFunctionNode node) {
		this.lazyNodeMap.put(node.GetSignature(), node);
	}

	protected void lazyBuild(String signature) {
		BNode node = this.lazyNodeMap.get(signature);
		if(node != null) {
			LibBunSystem._PrintDebug("LazyBuilding: " + signature);
			this.lazyNodeMap.remove(signature);
			node.Accept(this);
		}
	}

	public final Class<?> getJavaClass(BType zType, Class<?> C) {
		if(zType instanceof BFuncType) {
			return this.loadFuncClass((BFuncType)zType);
		}
		return this.typeTable.GetJavaClass(zType, C);
	}

	public final Class<?> getJavaClass(BType zType) {
		return this.getJavaClass(zType, Object.class);
	}

	@Override public BType GetFieldType(BType recvType, String fieldName) {
		Class<?> javaClass = this.getJavaClass(recvType);
		if(javaClass != null) {
			try {
				java.lang.reflect.Field javaField = javaClass.getField(fieldName);
				if(Modifier.isPublic(javaField.getModifiers())) {
					return this.typeTable.GetBunType(javaField.getType());
				}
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
			return BType.VoidType;     // undefined
		}
		return BType.VarType;     // undefined
	}

	@Override public BType GetSetterType(BType recvType, String fieldName) {
		Class<?> javaClass = this.getJavaClass(recvType);
		if(javaClass != null) {
			try {
				java.lang.reflect.Field javaField = javaClass.getField(fieldName);
				if(Modifier.isPublic(javaField.getModifiers()) && !Modifier.isFinal(javaField.getModifiers())) {
					return this.typeTable.GetBunType(javaField.getType());
				}
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
			return BType.VoidType;     // undefined
		}
		return BType.VarType;     // undefined
	}

	@Override public BFuncType GetMethodFuncType(BType recvType, String methodName, AbstractListNode paramList) {
		if(methodName == null) {
			Constructor<?> jMethod = this.javaTypeUtils.getConstructor(recvType, paramList);
			if(jMethod != null) {
				Class<?>[] paramTypes = jMethod.getParameterTypes();
				BArray<BType> typeList = new BArray<BType>(new BType[paramTypes.length + 2]);
				if (paramTypes != null) {
					int j = 0;
					while(j < LibBunSystem._Size(paramTypes)) {
						typeList.add(this.typeTable.GetBunType(paramTypes[j]));
						j = j + 1;
					}
				}
				typeList.add(recvType);
				return BTypePool._LookupFuncType2(typeList);
			}
		}
		else {
			Method jMethod = this.javaTypeUtils.getMethod(recvType, methodName, paramList);
			if(jMethod != null) {
				return this.typeTable.ConvertToFuncType(jMethod);
			}
		}
		return null;
	}

	@Override public void VisitNullNode(BunNullNode node) {
		this.methodBuilder.visitInsn(Opcodes.ACONST_NULL);
	}

	@Override public void VisitBooleanNode(BunBooleanNode node) {
		this.methodBuilder.pushBoolean(node.BooleanValue);
	}

	@Override public void VisitIntNode(BunIntNode node) {
		this.methodBuilder.pushLong(node.IntValue);
	}

	@Override public void VisitFloatNode(BunFloatNode node) {
		this.methodBuilder.pushDouble(node.FloatValue);
	}

	@Override public void VisitStringNode(BunStringNode node) {
		this.methodBuilder.visitLdcInsn(node.StringValue);
	}

	@Override public void VisitArrayLiteralNode(BunArrayLiteralNode node) {
		if(node.IsUntyped()) {
			this.VisitErrorNode(new ErrorNode(node, "ambigious array"));
			return;
		}
		Class<?> arrayClass = this.javaTypeUtils.asArrayClass(node.Type);
		String owner = Type.getInternalName(arrayClass);
		this.methodBuilder.visitTypeInsn(Opcodes.NEW, owner);
		this.methodBuilder.visitInsn(Opcodes.DUP);
		this.methodBuilder.pushInt(node.Type.TypeId);
		this.methodBuilder.pushNodeListAsArray(this.javaTypeUtils.asElementClass(node.Type), 0, node);
		this.methodBuilder.setLineNumber(node);
		this.methodBuilder.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", this.javaTypeUtils.newArrayDescriptor(node.Type));
	}

	@Override public void VisitMapLiteralNode(BunMapLiteralNode node) {
		if(node.IsUntyped()) {
			this.VisitErrorNode(new ErrorNode(node, "ambigious map"));
			return;
		}
		String owner = Type.getInternalName(BunMap.class);
		this.methodBuilder.visitTypeInsn(Opcodes.NEW, owner);
		this.methodBuilder.visitInsn(Opcodes.DUP);
		this.methodBuilder.pushInt(node.Type.TypeId);
		this.methodBuilder.pushInt(node.GetListSize() * 2);
		this.methodBuilder.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
		for(int i = 0; i < node.GetListSize() ; i++) {
			BunMapEntryNode entryNode = node.GetMapEntryNode(i);
			this.methodBuilder.visitInsn(Opcodes.DUP);
			this.methodBuilder.pushInt(i * 2);
			this.methodBuilder.pushNode(String.class, entryNode.KeyNode());
			this.methodBuilder.visitInsn(Opcodes.AASTORE);
			this.methodBuilder.visitInsn(Opcodes.DUP);
			this.methodBuilder.pushInt(i * 2 + 1);
			this.methodBuilder.pushNode(Object.class, entryNode.ValueNode());
			this.methodBuilder.visitInsn(Opcodes.AASTORE);
		}
		this.methodBuilder.setLineNumber(node);
		String desc = Type.getMethodDescriptor(Type.getType(void.class), new Type[] { Type.getType(int.class),  Type.getType(Object[].class)});
		this.methodBuilder.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", desc);
	}

	@Override public void VisitNewObjectNode(NewObjectNode node) {
		if(node.IsUntyped()) {
			this.VisitErrorNode(new ErrorNode(node, "no class for new operator"));
			return;
		}
		// check class existence
		if(!node.Type.Equals(this.typeTable.GetBunType(this.getJavaClass(node.Type)))) {
			this.VisitErrorNode(new ErrorNode(node, "undefined class: " + node.Type));
			return;
		}
		String className = Type.getInternalName(this.getJavaClass(node.Type));
		this.methodBuilder.visitTypeInsn(Opcodes.NEW, className);
		this.methodBuilder.visitInsn(Opcodes.DUP);
		Constructor<?> jMethod = this.javaTypeUtils.getConstructor(node.Type, node);
		if(jMethod == null) {
			this.VisitErrorNode(new ErrorNode(node, "no constructor: " + node.Type));
			return;
		}
		Class<?>[] paramClasses = jMethod.getParameterTypes();
		for(int i = 0; i < paramClasses.length; i++) {
			this.methodBuilder.pushNode(paramClasses[i], node.GetListAt(i));
		}
		this.methodBuilder.setLineNumber(node);
		this.methodBuilder.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", Type.getConstructorDescriptor(jMethod));
	}

	protected void VisitVarDeclNode(BunLetVarNode node) {
		String varName = node.GetGivenName();
		if(this.methodBuilder.findLocalVariable(varName) != null) {
			this.VisitErrorNode(new ErrorNode(node, varName + " is already defined"));
			return;
		}
		Class<?> declClass = this.getJavaClass(node.DeclType());
		this.methodBuilder.addLocal(declClass, varName);
		this.methodBuilder.pushNode(declClass, node.InitValueNode());
		this.methodBuilder.storeLocal(varName);
	}

	protected void VisitVarDeclNode2(BunLetVarNode node) {
		Class<?> declClass = this.getJavaClass(node.DeclType());
		this.methodBuilder.removeLocal(declClass, node.GetGivenName());
	}

	@Override public void VisitVarBlockNode(BunVarBlockNode node) {
		this.VisitVarDeclNode(node.VarDeclNode());
		this.VisitBlockNode(node);
		this.VisitVarDeclNode2(node.VarDeclNode());
	}

	public void VisitStaticFieldNode(JavaStaticFieldNode node) {
		this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(node.StaticClass), node.FieldName, this.getJavaClass(node.Type));
	}

	protected void VisitGlobalNameNode(GetNameNode node) {
		BunLetVarNode letNode = node.ResolvedNode;
		String varName = letNode.GetGivenName();
		int typeId = this.getTypeId(letNode.DeclType());
		int varIndex = GlobalVariableTable.getVarIndex(varName, typeId);
		if(varIndex == -1) {
			this.VisitErrorNode(new ErrorNode(node, "undefiend varibale: " + varName));
			return;
		}
		String owner = Type.getInternalName(GlobalVariableTable.class);
		switch(typeId) {
		case GlobalVariableTable.LONG_TYPE:
			this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "longVarTable", Type.getDescriptor(long[].class));
			this.methodBuilder.pushInt(varIndex);
			this.methodBuilder.visitInsn(Opcodes.LALOAD);
			break;
		case GlobalVariableTable.DOUBLE_TYPE:
			this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "doubleVarTable", Type.getDescriptor(double[].class));
			this.methodBuilder.pushInt(varIndex);
			this.methodBuilder.visitInsn(Opcodes.DALOAD);
			break;
		case GlobalVariableTable.BOOLEAN_TYPE:
			this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "booleanVarTable", Type.getDescriptor(boolean[].class));
			this.methodBuilder.pushInt(varIndex);
			this.methodBuilder.visitInsn(Opcodes.BALOAD);
			break;
		case GlobalVariableTable.OBJECT_TYPE:
			this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "objectVarTable", Type.getDescriptor(Object[].class));
			this.methodBuilder.pushInt(varIndex);
			this.methodBuilder.visitInsn(Opcodes.AALOAD);
			this.methodBuilder.visitTypeInsn(Opcodes.CHECKCAST, this.getJavaClass(node.Type));
			break;
		}
	}

	@Override public void VisitGetNameNode(GetNameNode node) {
		if(node.ResolvedNode == null) {
			this.VisitErrorNode(new ErrorNode(node, "undefined symbol: " + node.GivenName));
			return;
		}
		if(node.ResolvedNode.GetDefiningFunctionNode() == null) {
			this.VisitGlobalNameNode(node);
			return;
		}
		this.methodBuilder.loadLocal(node.GetUniqueName(this));
		this.methodBuilder.checkReturnCast(node, this.methodBuilder.getLocalType(node.GetUniqueName(this)));
	}

	protected void generateAssignNode(GetNameNode node, BNode exprNode) {
		if(node.ResolvedNode == null) {
			this.VisitErrorNode(new ErrorNode(node, "undefined symbol: " + node.GivenName));
			return;
		}
		if(node.ResolvedNode.IsReadOnly() && !(node.ResolvedNode.ParentNode instanceof BunFunctionNode)) {
			this.VisitErrorNode(new ErrorNode(node, "read only variable: " + node.GivenName));
			return;
		}
		if(node.ResolvedNode.GetDefiningFunctionNode() == null) {
			int typeId = this.getTypeId(exprNode.Type);
			int varIndex = GlobalVariableTable.getVarIndex(node.ResolvedNode.GetGivenName(), typeId);
			this.setVariable(varIndex, typeId, exprNode);
			return;
		}
		String name = node.GetUniqueName(this);
		this.methodBuilder.pushNode(this.methodBuilder.getLocalType(name), exprNode);
		this.methodBuilder.storeLocal(name);
	}


	@Override public void VisitAssignNode(AssignNode node) {
		BNode leftNode = node.LeftNode();
		if(leftNode instanceof GetNameNode) {
			this.generateAssignNode((GetNameNode)leftNode, node.RightNode());
		}
		else if(leftNode instanceof GetFieldNode) {
			this.generateAssignNode((GetFieldNode)leftNode, node.RightNode());
		}
		else if(leftNode instanceof GetIndexNode) {
			this.generateAssignNode((GetIndexNode)leftNode, node.RightNode());
		}
	}

	@Override public void VisitGroupNode(GroupNode node) {
		node.ExprNode().Accept(this);
	}

	private Field getField(Class<?> recvClass, String name) {
		try {
			return recvClass.getField(name);
		} catch (Exception e) {
			LibBunSystem._FixMe(e);
		}
		return null;  // type checker guarantees field exists
	}

	@Override public void VisitGetFieldNode(GetFieldNode node) {
		assert !node.IsUntyped();
		Class<?> recvClass = this.getJavaClass(node.RecvNode().Type);
		Field jField = this.getField(recvClass, node.GetName());
		String owner = Type.getType(recvClass).getInternalName();
		String desc = Type.getType(jField.getType()).getDescriptor();
		if(Modifier.isStatic(jField.getModifiers())) {
			this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, node.GetName(), desc);
		}
		else {
			this.methodBuilder.pushNode(null, node.RecvNode());
			this.methodBuilder.visitFieldInsn(Opcodes.GETFIELD, owner, node.GetName(), desc);
		}
		this.methodBuilder.checkReturnCast(node, jField.getType());
	}

	protected void generateAssignNode(GetFieldNode node, BNode exprNode) {
		assert !node.IsUntyped();
		Class<?> recvClass = this.getJavaClass(node.RecvNode().Type);
		Field jField = this.getField(recvClass, node.GetName());
		String owner = Type.getType(recvClass).getInternalName();
		String desc = Type.getType(jField.getType()).getDescriptor();
		if(Modifier.isStatic(jField.getModifiers())) {
			this.methodBuilder.pushNode(jField.getType(), exprNode);
			this.methodBuilder.visitFieldInsn(Opcodes.PUTSTATIC, owner, node.GetName(), desc);
		}
		else {
			this.methodBuilder.pushNode(null, node.RecvNode());
			this.methodBuilder.pushNode(jField.getType(), exprNode);
			this.methodBuilder.visitFieldInsn(Opcodes.PUTFIELD, owner, node.GetName(), desc);
		}
	}

	@Override public void VisitGetIndexNode(GetIndexNode node) {
		Method sMethod = this.methodTable.GetBinaryStaticMethod(node.RecvNode().Type, "[]", node.IndexNode().Type);
		this.methodBuilder.applyStaticMethod(node, sMethod, new BNode[] {node.RecvNode(), node.IndexNode()});
	}

	protected void generateAssignNode(GetIndexNode node, BNode exprNode) {
		Method sMethod = this.methodTable.GetBinaryStaticMethod(node.RecvNode().Type, "[]=", node.IndexNode().Type);
		this.methodBuilder.applyStaticMethod(node.ParentNode, sMethod, new BNode[] {node.RecvNode(), node.IndexNode(), exprNode});
	}

	private int getInvokeType(Method jMethod) {
		if(Modifier.isStatic(jMethod.getModifiers())) {
			return Opcodes.INVOKESTATIC;
		}
		if(Modifier.isInterface(jMethod.getModifiers())) {
			return Opcodes.INVOKEINTERFACE;
		}
		return Opcodes.INVOKEVIRTUAL;
	}

	@Override public void VisitMethodCallNode(MethodCallNode node) {
		this.methodBuilder.setLineNumber(node);
		Method jMethod = this.javaTypeUtils.getMethod(node.RecvNode().Type, node.MethodName(), node);
		assert jMethod != null;
		if(!Modifier.isStatic(jMethod.getModifiers())) {
			this.methodBuilder.pushNode(null, node.RecvNode());
		}
		Class<?>[] paramClasses = jMethod.getParameterTypes();
		for(int i = 0; i < paramClasses.length; i++) {
			this.methodBuilder.pushNode(paramClasses[i], node.GetListAt(i));
		}
		int inst = this.getInvokeType(jMethod);
		String owner = Type.getInternalName(jMethod.getDeclaringClass());
		this.methodBuilder.visitMethodInsn(inst, owner, jMethod.getName(), Type.getMethodDescriptor(jMethod));
		this.methodBuilder.checkReturnCast(node, jMethod.getReturnType());
	}

	@Override public void VisitFormNode(BunFormNode node) {
		for(int i = 0; i < node.GetListSize(); i++) {
			this.methodBuilder.pushNode(null, node.GetListAt(i));
		}
		String formText = node.FormFunc.FormText;
		int classEnd = formText.indexOf(".");
		int methodEnd = formText.indexOf("(");
		//System.out.println("FormText: " + FormText + " " + ClassEnd + ", " + MethodEnd);
		String className = formText.substring(0, classEnd);
		String methodName = formText.substring(classEnd+1, methodEnd);
		this.methodBuilder.setLineNumber(node);
		//System.out.println("debug: " + ClassName + ", " + MethodName);
		this.methodBuilder.visitMethodInsn(Opcodes.INVOKESTATIC, className, methodName, node.FormFunc.FuncType);
	}

	@Override public void VisitFuncCallNode(FuncCallNode node) {
		BType funcType = node.FunctorNode().Type;
		BunFuncNameNode funcNameNode = node.FuncNameNode();
		if(!(funcType instanceof BFuncType)) { // lookup func type
			BFunc func = ((BunTypeSafer)this.TypeChecker).LookupFunc(node.GetGamma(), funcNameNode.FuncName, funcNameNode.RecvType, funcNameNode.FuncParamSize);
			if(func != null) {
				funcType = func.GetFuncType();
				node.Type = ((BFuncType)funcType).GetReturnType();
			}
		}
		if(funcType instanceof BFuncType) {
			if(funcNameNode != null) {
				this.methodBuilder.applyFuncName(funcNameNode, funcNameNode.FuncName, (BFuncType)funcType, node);
			}
			else {
				Class<?> funcClass = this.loadFuncClass((BFuncType)funcType);
				this.methodBuilder.applyFuncObject(node, funcClass, node.FunctorNode(), (BFuncType)funcType, node);
			}
		}
		else {
			this.VisitErrorNode(new ErrorNode(node, "not function"));
		}
	}

	@Override public void VisitUnaryNode(UnaryOperatorNode node) {
		Method sMethod = this.methodTable.GetUnaryStaticMethod(node.SourceToken.GetText(), node.RecvNode().Type);
		this.methodBuilder.applyStaticMethod(node, sMethod, new BNode[] {node.RecvNode()});
	}

	@Override public void VisitNotNode(BunNotNode node) {
		this.VisitUnaryNode(node);
	}

	@Override public void VisitPlusNode(BunPlusNode node) {
		this.VisitUnaryNode(node);
	}

	@Override public void VisitMinusNode(BunMinusNode node) {
		this.VisitUnaryNode(node);
	}

	@Override public void VisitComplementNode(BunComplementNode node) {
		this.VisitUnaryNode(node);
	}

	@Override public void VisitCastNode(BunCastNode node) {
		if(node.Type.IsVoidType()) {
			node.ExprNode().Accept(this);
			this.methodBuilder.Pop(node.ExprNode().Type);
			return;
		}
		Class<?> targetClass = this.getJavaClass(node.Type);
		Class<?> sourceClass = this.getJavaClass(node.ExprNode().Type);
		Method sMethod = this.methodTable.GetCastMethod(targetClass, sourceClass);
		if(sMethod != null) {
			this.methodBuilder.applyStaticMethod(node, sMethod, new BNode[] {node.ExprNode()});
		}
		else if(!targetClass.isAssignableFrom(sourceClass)) {
			this.methodBuilder.visitTypeInsn(Opcodes.CHECKCAST, targetClass);
		}
	}

	@Override public void VisitInstanceOfNode(BunInstanceOfNode node) {
		if(!(node.LeftNode().Type instanceof BGenericType) && !(node.LeftNode().Type instanceof BFuncType)) {
			this.VisitNativeInstanceOfNode(node);
			return;
		}
		node.LeftNode().Accept(this);
		this.methodBuilder.Pop(node.LeftNode().Type);
		this.methodBuilder.pushLong(node.LeftNode().Type.TypeId);
		this.methodBuilder.pushLong(node.TargetType().TypeId);
		Method method = this.methodTable.GetBinaryStaticMethod(BType.IntType, "==", BType.IntType);
		this.invokeStaticMethod(null, method);
	}

	private void VisitNativeInstanceOfNode(BunInstanceOfNode node) {
		if(!node.TargetType().Equals(this.typeTable.GetBunType(this.getJavaClass(node.TargetType())))) {
			node.LeftNode().Accept(this);
			this.methodBuilder.Pop(node.LeftNode().Type);
			this.methodBuilder.pushBoolean(false);
			return;
		}
		Class<?> javaClass = this.getJavaClass(node.TargetType());
		if(node.TargetType().IsIntType()) {
			javaClass = Long.class;
		}
		else if(node.TargetType().IsFloatType()) {
			javaClass = Double.class;
		}
		else if(node.TargetType().IsBooleanType()) {
			javaClass = Boolean.class;
		}
		this.invokeBoxingMethod(node.LeftNode());
		this.methodBuilder.visitTypeInsn(Opcodes.INSTANCEOF, javaClass);
	}

	private void invokeBoxingMethod(BNode targetNode) {
		Class<?> targetClass = Object.class;
		if(targetNode.Type.IsIntType()) {
			targetClass = Long.class;
		}
		else if(targetNode.Type.IsFloatType()) {
			targetClass = Double.class;
		}
		else if(targetNode.Type.IsBooleanType()) {
			targetClass = Boolean.class;
		}
		Class<?> sourceClass = this.getJavaClass(targetNode.Type);
		Method sMethod = this.methodTable.GetCastMethod(targetClass, sourceClass);
		targetNode.Accept(this);
		if(!targetClass.equals(Object.class)) {
			this.invokeStaticMethod(null, sMethod);
		}
	}

	@Override public void VisitBinaryNode(BinaryOperatorNode node) {
		Method sMethod = this.methodTable.GetBinaryStaticMethod(node.LeftNode().Type, node.GetOperator(), node.RightNode().Type);
		this.methodBuilder.applyStaticMethod(node, sMethod, new BNode[] {node.LeftNode(), node.RightNode()});
	}

	@Override public void VisitAddNode(BunAddNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitSubNode(BunSubNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitMulNode(BunMulNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitDivNode(BunDivNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitModNode(BunModNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitLeftShiftNode(BunLeftShiftNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitRightShiftNode(BunRightShiftNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitBitwiseAndNode(BunBitwiseAndNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitBitwiseOrNode(BunBitwiseOrNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitBitwiseXorNode(BunBitwiseXorNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitEqualsNode(BunEqualsNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitNotEqualsNode(BunNotEqualsNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitLessThanNode(BunLessThanNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitLessThanEqualsNode(BunLessThanEqualsNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitGreaterThanNode(BunGreaterThanNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitGreaterThanEqualsNode(BunGreaterThanEqualsNode node) {
		this.VisitBinaryNode(node);
	}

	@Override public void VisitAndNode(BunAndNode node) {
		Label elseLabel = new Label();
		Label mergeLabel = new Label();
		this.methodBuilder.pushNode(boolean.class, node.LeftNode());
		this.methodBuilder.visitJumpInsn(Opcodes.IFEQ, elseLabel);

		this.methodBuilder.pushNode(boolean.class, node.RightNode());
		this.methodBuilder.visitJumpInsn(Opcodes.IFEQ, elseLabel);

		this.methodBuilder.visitLdcInsn(true);
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, mergeLabel);

		this.methodBuilder.visitLabel(elseLabel);
		this.methodBuilder.visitLdcInsn(false);
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, mergeLabel);

		this.methodBuilder.visitLabel(mergeLabel);
	}

	@Override public void VisitOrNode(BunOrNode node) {
		Label thenLabel = new Label();
		Label mergeLabel = new Label();
		this.methodBuilder.pushNode(boolean.class, node.LeftNode());
		this.methodBuilder.visitJumpInsn(Opcodes.IFNE, thenLabel);

		this.methodBuilder.pushNode(boolean.class, node.RightNode());
		this.methodBuilder.visitJumpInsn(Opcodes.IFNE, thenLabel);

		this.methodBuilder.visitLdcInsn(false);
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, mergeLabel);

		this.methodBuilder.visitLabel(thenLabel);
		this.methodBuilder.visitLdcInsn(true);
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, mergeLabel);

		this.methodBuilder.visitLabel(mergeLabel);
	}

	@Override public void VisitBlockNode(BunBlockNode node) {
		for (int i = 0; i < node.GetListSize(); i++) {
			node.GetListAt(i).Accept(this);
		}
	}

	@Override public void VisitIfNode(BunIfNode node) {
		Label elseLabel = new Label();
		Label endLabel = new Label();
		this.methodBuilder.pushNode(boolean.class, node.CondNode());
		this.methodBuilder.visitJumpInsn(Opcodes.IFEQ, elseLabel);
		// Then
		node.ThenNode().Accept(this);
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, endLabel);
		// Else
		this.methodBuilder.visitLabel(elseLabel);
		if(node.ElseNode() != null) {
			node.ElseNode().Accept(this);
			this.methodBuilder.visitJumpInsn(Opcodes.GOTO, endLabel);
		}
		// End
		this.methodBuilder.visitLabel(endLabel);
	}

	@Override public void VisitReturnNode(BunReturnNode node) {
		if(node.HasReturnExpr()) {
			node.ExprNode().Accept(this);
			Type type = this.javaTypeUtils.asmType(node.ExprNode().Type);
			this.methodBuilder.visitInsn(type.getOpcode(Opcodes.IRETURN));
		}
		else {
			this.methodBuilder.visitInsn(Opcodes.RETURN);
		}
	}

	@Override public void VisitWhileNode(BunWhileNode node) {
		if(node.HasNextNode()) {
			node.BlockNode().Append(node.NextNode());
		}
		Label continueLabel = new Label();
		Label breakLabel = new Label();
		this.methodBuilder.breakLabelStack.push(breakLabel);
		this.methodBuilder.continueLabelStack.push(continueLabel);

		this.methodBuilder.visitLabel(continueLabel);
		this.methodBuilder.pushNode(boolean.class, node.CondNode());
		this.methodBuilder.visitJumpInsn(Opcodes.IFEQ, breakLabel); // condition
		node.BlockNode().Accept(this);
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, continueLabel);
		this.methodBuilder.visitLabel(breakLabel);

		this.methodBuilder.breakLabelStack.pop();
		this.methodBuilder.continueLabelStack.pop();
	}

	@Override public void VisitBreakNode(BunBreakNode node) {
		Label l = this.methodBuilder.breakLabelStack.peek();
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, l);
	}

	@Override public void VisitThrowNode(BunThrowNode node) {
		node.ExprNode().Accept(this);
		this.methodBuilder.visitInsn(Opcodes.ATHROW);
	}

	@Override public void VisitTryNode(BunTryNode Node) {	//do nothing
	}


	private int getTypeId(BType type) {
		if(type.IsIntType()) {
			return GlobalVariableTable.LONG_TYPE;
		}
		else if(type.IsFloatType()) {
			return GlobalVariableTable.DOUBLE_TYPE;
		}
		else if(type.IsBooleanType()) {
			return GlobalVariableTable.BOOLEAN_TYPE;
		}
		else {
			return GlobalVariableTable.OBJECT_TYPE;
		}
	}

	private void setVariable(int varIndex, int typeId, BNode valueNode) {
		String owner = Type.getInternalName(GlobalVariableTable.class);
		switch(typeId) {
		case GlobalVariableTable.LONG_TYPE:
			this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "longVarTable", Type.getDescriptor(long[].class));
			this.methodBuilder.pushInt(varIndex);
			this.methodBuilder.pushNode(long.class, valueNode);
			this.methodBuilder.visitInsn(Opcodes.LASTORE);
			break;
		case GlobalVariableTable.DOUBLE_TYPE:
			this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "doubleVarTable", Type.getDescriptor(double[].class));
			this.methodBuilder.pushInt(varIndex);
			this.methodBuilder.pushNode(double.class, valueNode);
			this.methodBuilder.visitInsn(Opcodes.DASTORE);
			break;
		case GlobalVariableTable.BOOLEAN_TYPE:
			this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "booleanVarTable", Type.getDescriptor(boolean[].class));
			this.methodBuilder.pushInt(varIndex);
			this.methodBuilder.pushNode(boolean.class, valueNode);
			this.methodBuilder.visitInsn(Opcodes.BASTORE);
			break;
		case GlobalVariableTable.OBJECT_TYPE:
			this.methodBuilder.visitFieldInsn(Opcodes.GETSTATIC, owner, "objectVarTable", Type.getDescriptor(Object[].class));
			this.methodBuilder.pushInt(varIndex);
			this.methodBuilder.pushNode(null, valueNode);
			this.methodBuilder.visitInsn(Opcodes.AASTORE);
			break;
		}
	}

	@Override public void VisitLetNode(BunLetVarNode node) {
		String varName = node.GetGivenName();
		if(GlobalVariableTable.existEntry(varName)) {
			this.VisitErrorNode(new ErrorNode(node, varName + " is already defined"));
			return;
		}
		int typeId = this.getTypeId(node.DeclType());
		int varIndex = GlobalVariableTable.addEntry(varName, typeId, node.IsReadOnly());
		try {
			this.setVariable(varIndex, typeId, node.InitValueNode());
		}
		catch(Throwable t) {
			GlobalVariableTable.removeEntry(varName);
			if(!(t instanceof RuntimeException)) {
				t = new RuntimeException(t);
			}
			throw (RuntimeException)t;
		}
	}

	Class<?> loadFuncClass(BFuncType funcType) {
		String className = this.NameType(funcType);
		Class<?> funcClass = this.getGeneratedClass(className, null);
		if(funcClass == null) {
			String superClassName = Type.getInternalName(BFunction.class);
			ClassBuilder classBuilder = this.asmLoader.newClassBuilder(Opcodes.ACC_PUBLIC| Opcodes.ACC_ABSTRACT, null, className, BFunction.class);
			MethodBuilder invokeMethodBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "Invoke", funcType);
			invokeMethodBuilder.finish();

			MethodBuilder initMethodBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC, "<init>", "(ILjava/lang/String;)V");
			initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 0);
			initMethodBuilder.visitVarInsn(Opcodes.ILOAD, 1);
			initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 2);
			initMethodBuilder.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, "<init>", "(ILjava/lang/String;)V");
			initMethodBuilder.visitInsn(Opcodes.RETURN);
			initMethodBuilder.finish();

			funcClass = this.asmLoader.loadGeneratedClass(className);
			this.setGeneratedClass(className, funcClass);
		}
		return funcClass;
	}

	@Override public void VisitFunctionNode(BunFunctionNode node) {
		if(node.IsTopLevelDefineFunction()) {
			assert(node.FuncName() != null);
			assert(node.IsTopLevel());  // otherwise, transformed to var f = function ()..
			JavaStaticFieldNode funcNode = this.generateFunctionAsSymbolField(node.FuncName(), node);
			if(node.IsExport) {
				if(node.FuncName().equals("main")) {
					this.mainFuncNode = funcNode;
				}
			}
			this.setMethod(node.FuncName(), (BFuncType)funcNode.Type, funcNode.StaticClass);
		}
		else {
			JavaStaticFieldNode funcNode = this.generateFunctionAsSymbolField(node.GetUniqueName(this), node);
			if(this.methodBuilder != null) {
				this.VisitStaticFieldNode(funcNode);
			}
		}
	}

	private JavaStaticFieldNode generateFunctionAsSymbolField(String funcName, BunFunctionNode node) {
		BFuncType funcType = node.GetFuncType();
		String className = this.NameFunctionClass(funcName, funcType);
		Class<?> funcClass = this.loadFuncClass(funcType);
		ClassBuilder classBuilder = this.asmLoader.newClassBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, node, className, funcClass);

		MethodBuilder invokeMethodBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "Invoke", funcType);
		int index = 1;
		for(int i = 0; i < funcType.GetFuncParamSize(); i++) {
			Type asmType = this.javaTypeUtils.asmType(funcType.GetFuncParamType(i));
			invokeMethodBuilder.visitVarInsn(asmType.getOpcode(Opcodes.ILOAD), index);
			index += asmType.getSize();
		}
		invokeMethodBuilder.visitMethodInsn(Opcodes.INVOKESTATIC, className, "f", funcType);
		invokeMethodBuilder.visitReturn(funcType.GetReturnType());
		invokeMethodBuilder.finish();

		classBuilder.addField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "function", funcClass, null);

		// static init
		MethodBuilder staticInitBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC , "<clinit>", "()V");
		staticInitBuilder.visitTypeInsn(Opcodes.NEW, className);
		staticInitBuilder.visitInsn(Opcodes.DUP);
		staticInitBuilder.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", "()V");
		staticInitBuilder.visitFieldInsn(Opcodes.PUTSTATIC, className, "function",  funcClass);
		staticInitBuilder.visitInsn(Opcodes.RETURN);
		staticInitBuilder.finish();

		MethodBuilder initMethodBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PRIVATE, "<init>", "()V");
		initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 0);
		initMethodBuilder.visitLdcInsn(funcType.TypeId);
		initMethodBuilder.visitLdcInsn(funcName);
		initMethodBuilder.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(funcClass), "<init>", "(ILjava/lang/String;)V");
		initMethodBuilder.visitInsn(Opcodes.RETURN);
		initMethodBuilder.finish();

		MethodBuilder staticFuncMethodBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "f", funcType);
		for(int i = 0; i < node.GetListSize(); i++) {
			BunLetVarNode paramNode = node.GetParamNode(i);
			Class<?> declClass = this.getJavaClass(paramNode.DeclType());
			staticFuncMethodBuilder.addLocal(declClass, paramNode.GetGivenName());
		}
		node.BlockNode().Accept(this);
		staticFuncMethodBuilder.finish();

		funcClass = this.asmLoader.loadGeneratedClass(className);
		this.setGeneratedClass(className, funcClass);
		return new JavaStaticFieldNode(null, funcClass, funcType, "function");
	}

	private BFunction loadFunction(Class<?> wrapperClass, Class<?> staticMethodClass) {
		try {
			Field f = staticMethodClass.getField("function");
			Object func = f.get(null);
			if(wrapperClass != null) {
				Constructor<?> c = wrapperClass.getConstructor(func.getClass().getSuperclass());
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

	private void setMethod(String funcName, BFuncType funcType, Class<?> funcClass) {
		BType recvType = funcType.GetRecvType();
		if(recvType instanceof BClassType && funcName != null) {
			BClassType classType = (BClassType)recvType;
			BType fieldType = classType.GetFieldType(funcName, null);
			if(fieldType == null || !fieldType.IsFuncType()) {
				funcName = LibBunSystem._AnotherName(funcName);
				fieldType = classType.GetFieldType(funcName, null);
				if(fieldType == null || !fieldType.IsFuncType()) {
					return;
				}
			}
			if(fieldType.Equals(funcType)) {
				this.setMethod(classType, funcName, this.loadFunction(null, funcClass));
			}
			else if(this.isMethodFuncType((BFuncType)fieldType, funcType)) {
				Class<?> WrapperClass = this.methodWrapperClass((BFuncType)fieldType, funcType);
				this.setMethod(classType, funcName, this.loadFunction(WrapperClass, funcClass));
			}
		}
	}

	private boolean isMethodFuncType(BFuncType fieldType, BFuncType funcType) {
		if(funcType.GetFuncParamSize() == fieldType.GetFuncParamSize() && funcType.GetReturnType().Equals(fieldType.GetReturnType())) {
			for(int i = 1; i < funcType.GetFuncParamSize(); i++) {
				if(!funcType.GetFuncParamType(i).Equals(fieldType.GetFuncParamType(i))) {
					return false;
				}
			}
		}
		return true;
	}

	private Class<?> methodWrapperClass(BFuncType funcType, BFuncType sourceFuncType) {
		String className = "W" + this.NameType(funcType) + "W" + this.NameType(sourceFuncType);
		Class<?> wrapperClass = this.getGeneratedClass(className, null);
		if(wrapperClass == null) {
			Class<?> funcClass = this.loadFuncClass(funcType);
			Class<?> sourceClass = this.loadFuncClass(sourceFuncType);
			ClassBuilder classBuilder = this.asmLoader.newClassBuilder(Opcodes.ACC_PUBLIC|Opcodes.ACC_FINAL, null, className, funcClass);

			classBuilder.addField(Opcodes.ACC_PUBLIC, "f", sourceClass, null);

			MethodBuilder initMethodBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC, "<init>", "(L"+Type.getInternalName(sourceClass)+";)V");
			initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 0);
			initMethodBuilder.pushInt(funcType.TypeId);
			initMethodBuilder.visitLdcInsn(sourceFuncType.GetName());
			initMethodBuilder.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(funcClass), "<init>", "(ILjava/lang/String;)V");
			initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 0);
			initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 1);
			initMethodBuilder.visitFieldInsn(Opcodes.PUTFIELD, className, "f", Type.getDescriptor(sourceClass));
			initMethodBuilder.visitInsn(Opcodes.RETURN);
			initMethodBuilder.finish();

			MethodBuilder invokeMethodBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "Invoke", funcType);
			invokeMethodBuilder.visitVarInsn(Opcodes.ALOAD, 0);
			invokeMethodBuilder.visitFieldInsn(Opcodes.GETFIELD, className, "f", Type.getDescriptor(sourceClass));
			invokeMethodBuilder.visitVarInsn(Opcodes.ALOAD, 1);
			//			System.out.println("CAST: " + Type.getInternalName(this.GetJavaClass(SourceFuncType.GetFuncParamType(0))));
			invokeMethodBuilder.visitTypeInsn(Opcodes.CHECKCAST, this.getJavaClass(sourceFuncType.GetFuncParamType(0)));
			int index = 2;
			for(int i = 1; i < funcType.GetFuncParamSize(); i++) {
				Type asmType = this.javaTypeUtils.asmType(funcType.GetFuncParamType(i));
				invokeMethodBuilder.visitVarInsn(asmType.getOpcode(Opcodes.ILOAD), index);
				index += asmType.getSize();
			}
			//String owner = "C" + FuncType.StringfySignature(FuncName);
			invokeMethodBuilder.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(sourceClass), "Invoke", this.javaTypeUtils.getMethodDescriptor(sourceFuncType));
			invokeMethodBuilder.visitReturn(funcType.GetReturnType());
			invokeMethodBuilder.finish();

			wrapperClass = this.asmLoader.loadGeneratedClass(className);
			this.setGeneratedClass(className, wrapperClass);
		}
		return wrapperClass;
	}


	// -----------------------------------------------------------------------

	private Class<?> getSuperClass(BType superType) {
		Class<?> superClass = null;
		if(superType != null) {
			superClass = this.getJavaClass(superType);
		}
		else {
			superClass = BunObject.class;
		}
		return superClass;
	}

	private final static String nameClassMethod(BType classType, String fieldName) {
		return fieldName + classType.TypeId;
	}

	private void setMethod(BClassType classType, String funcName, BFunction funcObject) {
		try {
			Class<?> staticClass = this.getJavaClass(classType);
			Field f = staticClass.getField(nameClassMethod(classType, funcName));
			f.set(null, funcObject);
		}
		catch (Exception e) {
			e.printStackTrace();
			LibBunSystem._Exit(1, "failed " + e);
		}
	}

	private Object getConstValue(BNode node) {
		if(node instanceof BunNullNode) {
			return null;
		}
		if(node instanceof BunBooleanNode) {
			return ((BunBooleanNode)node).BooleanValue;
		}
		if(node instanceof BunIntNode) {
			return ((BunIntNode)node).IntValue;
		}
		if(node instanceof BunFloatNode) {
			return ((BunFloatNode)node).FloatValue;
		}
		if(node instanceof BunStringNode) {
			return ((BunStringNode)node).StringValue;
		}
		if(node instanceof BunTypeNode) {
			return node.Type;
		}
		return null;
	}

	@Override public void VisitClassNode(BunClassNode node) {
		Class<?> superClass = this.getSuperClass(node.SuperType());
		ClassBuilder classBuilder = this.asmLoader.newClassBuilder(Opcodes.ACC_PUBLIC, node, node.ClassName(), superClass);
		// add class field
		for(int i = 0; i < node.GetListSize(); i++) {
			BunLetVarNode fieldNode = node.GetFieldNode(i);
			classBuilder.addField(Opcodes.ACC_PUBLIC, fieldNode.GetGivenName(), fieldNode.DeclType(), this.getConstValue(fieldNode.InitValueNode()));
		}
		// add static field (only function)
		for(int i = 0; i < node.ClassType.GetFieldSize(); i++) {
			BClassField field = node.ClassType.GetFieldAt(i);
			if(field.FieldType.IsFuncType()) {
				classBuilder.addField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, nameClassMethod(node.ClassType, field.FieldName), field.FieldType, null);
			}
		}
		// public <init>()
		MethodBuilder initMethodBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PUBLIC, "<init>", "()V");
		initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 0);
		initMethodBuilder.pushInt(node.ClassType.TypeId);
		initMethodBuilder.visitMethodInsn(Opcodes.INVOKESPECIAL, node.ClassName(), "<init>", "(I)V");
		initMethodBuilder.visitInsn(Opcodes.RETURN);
		initMethodBuilder.finish();
		// protected <init>(int typeid)
		initMethodBuilder = classBuilder.newMethodBuilder(Opcodes.ACC_PROTECTED, "<init>", "(I)V");
		initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 0);
		//		InitMethod.visitVarInsn(Opcodes.ILOAD, 1);
		//		InitMethod.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(SuperClass), "<init>", "(I)V");
		initMethodBuilder.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(superClass), "<init>", "()V");	// FIXME: ZObject?
		for(int i = 0; i < node.GetListSize(); i++) {
			BunLetVarNode fieldNode = node.GetFieldNode(i);
			if(!fieldNode.DeclType().IsFuncType()) {
				initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 0);
				initMethodBuilder.pushNode(this.getJavaClass(fieldNode.DeclType()), fieldNode.InitValueNode());
				initMethodBuilder.visitFieldInsn(Opcodes.PUTFIELD, node.ClassName(), fieldNode.GetGivenName(), Type.getDescriptor(this.getJavaClass(fieldNode.DeclType())));
			}
		}
		// set function
		for(int i = 0; i < node.ClassType.GetFieldSize(); i++) {
			BClassField field = node.ClassType.GetFieldAt(i);
			if(field.FieldType.IsFuncType()) {
				String fieldDesc = Type.getDescriptor(this.getJavaClass(field.FieldType));
				Label jumpLabel = new Label();
				initMethodBuilder.visitFieldInsn(Opcodes.GETSTATIC, node.ClassName(), nameClassMethod(node.ClassType, field.FieldName), fieldDesc);
				initMethodBuilder.visitJumpInsn(Opcodes.IFNULL, jumpLabel);
				initMethodBuilder.visitVarInsn(Opcodes.ALOAD, 0);
				initMethodBuilder.visitFieldInsn(Opcodes.GETSTATIC, node.ClassName(), nameClassMethod(node.ClassType, field.FieldName), fieldDesc);
				initMethodBuilder.visitFieldInsn(Opcodes.PUTFIELD, node.ClassName(), field.FieldName, fieldDesc);
				initMethodBuilder.visitLabel(jumpLabel);
			}
		}

		initMethodBuilder.visitInsn(Opcodes.RETURN);
		initMethodBuilder.finish();

		this.typeTable.SetTypeTable(node.ClassType, this.asmLoader.loadGeneratedClass(node.ClassName()));
	}

	@Override public void VisitErrorNode(ErrorNode node) {
		assert node.SourceToken != null;
		LibBunLogger._LogError(node.SourceToken, node.ErrorMessage);
		throw new ErrorNodeFoundException();
	}

	@Override public void VisitAsmNode(BunAsmNode Node) {
		// TODO Auto-generated method stub
	}

	@Override public void VisitTopLevelNode(TopLevelNode node) {
		this.VisitUndefinedNode(node);
	}

	@Override public void VisitLocalDefinedNode(LocalDefinedNode node) {
		if(node instanceof JavaStaticFieldNode) {
			this.VisitStaticFieldNode(((JavaStaticFieldNode)node));
		}
		else {
			this.VisitUndefinedNode(node);
		}
	}

	public final void debugPrint(String message) {
		LibBunSystem._PrintDebug(message);
	}

	@Override public void VisitLiteralNode(LiteralNode node) {
	}

	public JavaMethodTable getMethodTable() {
		return this.methodTable;
	}

	public JavaTypeTable getTypeTable() {
		return this.typeTable;
	}

	@Override
	public void visitCommandNode(CommandNode node) {
		this.methodBuilder.setLineNumber(node);
		ArrayList<CommandNode> nodeList = new ArrayList<CommandNode>();
		CommandNode commandNode = node;
		while(commandNode != null) {
			nodeList.add(commandNode);
			commandNode = (CommandNode) commandNode.getPipedNextNode();
		}
		// new String[n][]
		int size = nodeList.size();
		this.methodBuilder.visitLdcInsn(size);
		this.methodBuilder.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(CommandArg[].class));
		for(int i = 0; i < size; i++) {
			// new String[m];
			CommandNode currentNode = nodeList.get(i);
			int listSize = currentNode.getArgSize();
			this.methodBuilder.visitInsn(Opcodes.DUP);
			this.methodBuilder.visitLdcInsn(i);
			this.methodBuilder.visitLdcInsn(listSize);
			this.methodBuilder.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(CommandArg.class));
			for(int j = 0; j < listSize; j++ ) {
				this.methodBuilder.visitInsn(Opcodes.DUP);
				this.methodBuilder.visitLdcInsn(j);
				currentNode.getArgAt(j).Accept(this);
				this.methodBuilder.visitInsn(Opcodes.AASTORE);
			}
			this.methodBuilder.visitInsn(Opcodes.AASTORE);
		}

		if(node.Type.IsBooleanType()) {
			this.invokeStaticMethod(node, this.execCommandBool);
		}
		else if(node.Type.IsIntType()) {
			this.invokeStaticMethod(node, this.execCommandInt);
		}
		else if(node.Type.IsStringType()) {
			this.invokeStaticMethod(node, this.execCommandString);
		}
		else if(node.Type.equals(BTypePool._GetGenericType1(BGenericType._ArrayType, BType.StringType))) {
			this.invokeStaticMethod(node, this.execCommandStringArray);
		}
		else if(node.Type.equals(this.typeTable.GetBunType(Task.class))) {
			this.invokeStaticMethod(node, this.execCommandTask);
		}
		else if(node.Type.equals(BTypePool._GetGenericType1(BGenericType._ArrayType, this.typeTable.GetBunType(Task.class)))) {
			this.invokeStaticMethod(node, this.execCommandTaskArray);
		}
		else {
			this.invokeStaticMethod(node, this.execCommandVoid);
		}
	}

	@Override
	public void visitTryNode(DShellTryNode node) {
		TryCatchLabel label = new TryCatchLabel();
		this.methodBuilder.tryCatchLabelStack.push(label); // push
		// try block
		this.methodBuilder.visitLabel(label.BeginTryLabel);
		node.tryBlockNode().Accept(this);
		this.methodBuilder.visitLabel(label.EndTryLabel);
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, label.FinallyLabel);
		// catch block
		int size = node.GetListSize();
		for(int i = 0; i < size; i++) {
			node.GetListAt(i).Accept(this);
		}
		// finally block
		this.methodBuilder.visitLabel(label.FinallyLabel);
		if(node.hasFinallyBlockNode()) {
			node.finallyBlockNode().Accept(this);
		}
		this.methodBuilder.tryCatchLabelStack.pop();
	}

	@Override
	public void visitCatchNode(DShellCatchNode node) {
		Label catchLabel = new Label();
		TryCatchLabel Label = this.methodBuilder.tryCatchLabelStack.peek();

		// prepare
		String throwType = this.resolveExceptionType(node);
		this.methodBuilder.visitTryCatchBlock(Label.BeginTryLabel, Label.EndTryLabel, catchLabel, throwType);

		// catch block
		this.methodBuilder.addLocal(this.getJavaClass(node.exceptionType()), node.exceptionName());
		this.methodBuilder.visitLabel(catchLabel);
		this.invokeExceptionWrapper(node);
		node.blockNode().Accept(this);
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, Label.FinallyLabel);

		this.methodBuilder.removeLocal(this.getJavaClass(node.exceptionType()), node.exceptionName());
	}

	private String resolveExceptionType(DShellCatchNode node) {
		if(!node.hasTypeInfo()) {
			return Type.getType(Throwable.class).getInternalName();
		}
		return this.javaTypeUtils.asmType(node.exceptionType()).getInternalName();
	}

	private void invokeExceptionWrapper(DShellCatchNode node) {
		if(!node.hasTypeInfo()) {
			this.invokeStaticMethod(null, this.wrapException);
		}
		this.methodBuilder.storeLocal(node.exceptionName());
	}

	@Override public void VisitSyntaxSugarNode(SyntaxSugarNode node) {
		if(node instanceof BunContinueNode) {
			this.visitContinueNode((BunContinueNode) node);
		}
		else {
			super.VisitSyntaxSugarNode(node);
		}
	}

	@Override
	public void visitContinueNode(BunContinueNode node) {
		Label l = this.methodBuilder.continueLabelStack.peek();
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, l);
	}

	@Override
	public void visitForNode(DShellForNode node) {
		Label headLabel = new Label();
		Label continueLabel = new Label();
		Label breakLabel = new Label();
		this.methodBuilder.breakLabelStack.push(breakLabel);
		this.methodBuilder.continueLabelStack.push(continueLabel);

		if(node.hasDeclNode()) {
			this.VisitVarDeclNode(node.toVarDeclNode());
		}
		this.methodBuilder.visitLabel(headLabel);
		node.condNode().Accept(this);
		this.methodBuilder.visitJumpInsn(Opcodes.IFEQ, breakLabel);
		node.blockNode().Accept(this);
		this.methodBuilder.visitLabel(continueLabel);
		if(node.hasNextNode()) {
			node.nextNode().Accept(this);
		}
		this.methodBuilder.visitJumpInsn(Opcodes.GOTO, headLabel);
		this.methodBuilder.visitLabel(breakLabel);
		if(node.hasDeclNode()) {
			this.VisitVarDeclNode2(node.toVarDeclNode());
		}

		this.methodBuilder.breakLabelStack.pop();
		this.methodBuilder.continueLabelStack.pop();
	}

	@Override
	public void visitWrapperNode(DShellWrapperNode node) {
		node.getTargetNode().Accept(this);
	}

	@Override
	public void visitMatchRegexNode(MatchRegexNode node) {
		this.VisitBinaryNode(node);
	}

	@Override
	public void visitInternalFuncCallNode(InternalFuncCallNode node) {
		this.invokeStaticMethod(null, node.getMethod());
	}

	// utils for visitor
	protected void invokeStaticMethod(BNode Node, Method method) {
		String owner = Type.getInternalName(method.getDeclaringClass());
		this.methodBuilder.visitMethodInsn(Opcodes.INVOKESTATIC, owner, method.getName(), Type.getMethodDescriptor(method));
		if(Node != null) {
			this.methodBuilder.checkReturnCast(Node, method.getReturnType());
		}
	}

	protected void loadJavaClass(Class<?> classObject) {
		BType type = this.typeTable.GetBunType(classObject);
		this.RootGamma.SetTypeName(type, null);
	}

	protected void loadJavaClassList(ArrayList<Class<?>> classObjList) {
		for(Class<?> classObj : classObjList) {
			this.loadJavaClass(classObj);
		}
	}

	protected void loadJavaStaticMethod(Class<?> holderClass, String internalName, Class<?>... paramClasses) {
		this.loadJavaStaticMethod(holderClass, internalName, internalName, paramClasses);
	}

	protected void loadJavaStaticMethod(Class<?> holderClass, String name, String internalName, Class<?>... paramClasses) {
		this.loadJavaStaticMethod(holderClass, null, name, internalName, this.toBTypes(paramClasses));
	}

	protected void loadJavaStaticMethod(Class<?> holderClass, BType returnType, String name, BType... paramTypes) {
		this.loadJavaStaticMethod(holderClass, returnType, name, name, paramTypes);
	}

	protected void loadJavaStaticMethod(Class<?> holderClass, BType returnType, String name, String internalName, BType... paramTypes) {
		String formSymbol = name;
		String holderClassPath = holderClass.getCanonicalName().replaceAll("\\.", "/");
		BArray<BType> typeList = new BArray<BType>(new BType[4]);
		StringBuilder formBuilder = new StringBuilder();
		formBuilder.append(holderClassPath + "." + internalName + "(");
		for(int i = 0; i < paramTypes.length; i++) {
			if(i != 0) {
				formBuilder.append(",");
			}
			formBuilder.append("$[" + i + "]");
			typeList.add(paramTypes[i]);
		}
		formBuilder.append(")");
		if(returnType == null) {
			try {
				returnType = this.typeTable.GetBunType(holderClass.getMethod(internalName, this.toClasses(paramTypes)).getReturnType());
			}
			catch(Throwable e) {
				Utils.fatal(1, "load static method faild: " + e.getMessage());
			}
		}
		typeList.add(returnType);
		BFuncType funcType = (BFuncType) BTypePool._GetGenericType(BFuncType._FuncType, typeList, true);
		BFormFunc formFunc = new BFormFunc(formSymbol, funcType, null, formBuilder.toString());
		if(name.equals("_")) {
			this.SetConverterFunc(funcType.GetRecvType(), funcType.GetReturnType(), formFunc);
		}
		else {
			this.SetDefinedFunc(formFunc);
		}
	}

	protected Class<?>[] toClasses(BType[] types) {
		int size = types.length;
		Class<?>[] classes = new Class<?>[size];
		for(int i = 0; i < size; i++) {
			classes[i] = this.getJavaClass(types[i]);
		}
		return classes;
	}

	protected BType[] toBTypes(Class<?>[] classes) {
		int size = classes.length;
		BType[] types = new BType[size];
		for(int i = 0; i < size; i++) {
			types[i] = this.typeTable.GetBunType(classes[i]);
		}
		return types;
	}

	protected void clearCurrentFunction(BNode node) {	//TODO:
		//Class<?> FuncClass = this.GetDefinedFunctionClass(info.funcName, BType.VoidType, 0);
		//this.generatedClassMap.remove(this.NameFunctionClass(nodefuncName, funcType));
	}

	protected Class<?> removeDefinedFuncClass(String funcName, BType recvType, int funcParamSize) {
		return this.generatedClassMap.remove(this.NameFunctionClass(funcName, recvType, funcParamSize));
	}

	public JavaStaticFieldNode getMainFuncNode() {
		return this.mainFuncNode;
	}

	public static class ErrorNodeFoundException extends RuntimeException {
		private static final long serialVersionUID = -2465006344250569543L;
	}
}
