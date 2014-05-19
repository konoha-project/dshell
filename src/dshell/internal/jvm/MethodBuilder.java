package dshell.internal.jvm;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.BASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LASTORE;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Stack;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.type.BFuncType;
import libbun.type.BType;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;


class MethodBuilder extends MethodNode {
	final MethodBuilder        parent;
	final JavaByteCodeGenerator        generator;
	ArrayList<JavaLocalStack>     localVals  = new ArrayList<JavaLocalStack>();
	int UsedStack = 0;
	Stack<Label>                  breakLabelStack = new Stack<Label>();
	Stack<Label>                  continueLabelStack = new Stack<Label>();
	int PreviousLine = 0;

	public MethodBuilder(int acc, String name, String desc, JavaByteCodeGenerator generator) {
		super(acc, name, desc, null, null);
		this.generator = generator;
		this.parent = generator.AsmBuilder;
		generator.AsmBuilder = this;
	}

	public void finish() {
		assert(this.generator.AsmBuilder == this);
		this.generator.AsmBuilder = this.parent;
	}

	void setLineNumber(int line) {
		if(line != 0 && line != this.PreviousLine) {
			Label LineLabel = new Label();
			this.visitLabel(LineLabel);
			this.visitLineNumber(line, LineLabel);
			this.PreviousLine = line;
		}
	}

	void setLineNumber(BNode node) {
		if(node != null && node.SourceToken != null) {
			this.setLineNumber(node.SourceToken.GetLineNumber());
		}
	}

	void pushBoolean(boolean b) {
		if(b) {
			this.visitInsn(Opcodes.ICONST_1);
		}
		else {
			this.visitInsn(Opcodes.ICONST_0);
		}
	}

	void pushInt(int n) {
		switch(n) {
		case -1: this.visitInsn(Opcodes.ICONST_M1); return;
		case 0: this.visitInsn(Opcodes.ICONST_0); return;
		case 1: this.visitInsn(Opcodes.ICONST_1); return;
		case 2: this.visitInsn(Opcodes.ICONST_2); return;
		case 3: this.visitInsn(Opcodes.ICONST_3); return;
		case 4: this.visitInsn(Opcodes.ICONST_4); return;
		case 5: this.visitInsn(Opcodes.ICONST_5); return;
		default:
			if(n >= Byte.MIN_VALUE && n <= Byte.MAX_VALUE) {
				this.visitIntInsn(Opcodes.BIPUSH, n);
			}
			else if(n >= Short.MIN_VALUE && n <= Short.MAX_VALUE) {
				this.visitIntInsn(Opcodes.SIPUSH, n);
			}
			else {
				this.visitLdcInsn(n);
			}
		}
	}

	void pushLong(long n) {
		if(n == 0) {
			this.visitInsn(Opcodes.LCONST_0);
		}
		else if(n == 1) {
			this.visitInsn(Opcodes.LCONST_1);
		}
		else if(n >= Short.MIN_VALUE && n <= Short.MAX_VALUE) {
			this.pushInt((int)n);
			this.visitInsn(Opcodes.I2L);
		}
		else {
			this.visitLdcInsn(n);
		}
	}

	void pushDouble(double n) {
		if(n == 0.0) {
			this.visitInsn(Opcodes.DCONST_0);
		}
		else if(n == 1.0) {
			this.visitInsn(Opcodes.DCONST_1);
		}
		else {
			this.visitLdcInsn(n);
		}
	}


	void pushConst(Object value) {
		if(value instanceof Boolean) {
			this.pushBoolean((Boolean)value);
		}
		else if(value instanceof Long) {
			this.pushLong((Long)value);
		}
		else if(value instanceof Double) {
			this.pushDouble((Double)value);
		}
		else {
			this.visitLdcInsn(value);
		}
	}

	void Pop(BType T) {
		if(T.IsFloatType() || T.IsIntType()) {
			this.visitInsn(Opcodes.POP2);
		}
		else if(!T.IsVoidType()) {
			this.visitInsn(Opcodes.POP);
		}
	}

	JavaLocalStack addLocal(Class<?> jClass, String name) {
		Type asmType =  Type.getType(jClass);
		JavaLocalStack local = new JavaLocalStack(this.UsedStack, jClass, asmType, name);
		this.UsedStack = this.UsedStack + asmType.getSize();
		this.localVals.add(local);
		return local;
	}

	void removeLocal(Class<?> jType, String name) {
		for(int i = this.localVals.size() - 1; i >= 0; i--) {
			JavaLocalStack Local = this.localVals.get(i);
			if(Local.Name.equals(name)) {
				this.localVals.remove(i);
				return;
			}
		}
	}

	JavaLocalStack findLocalVariable(String name) {
		for(int i = 0; i < this.localVals.size(); i++) {
			JavaLocalStack l = this.localVals.get(i);
			if(l.Name.equals(name)) {
				return l;
			}
		}
		return null;
	}

	Class<?> getLocalType(String name) {
		JavaLocalStack local = this.findLocalVariable(name);
		return local.JavaType;
	}

	void loadLocal(String name) {
		JavaLocalStack local = this.findLocalVariable(name);
		Type type = local.AsmType;
		this.visitVarInsn(type.getOpcode(ILOAD), local.Index);
	}

	void storeLocal(String name) {
		JavaLocalStack local = this.findLocalVariable(name);
		Type type = local.AsmType;
		this.visitVarInsn(type.getOpcode(ISTORE), local.Index);
	}

	void checkCast(Class<?> targetClass, Class<?> sourceClass) {
		if(targetClass.equals(sourceClass)) {
			return;
		}
		Method sMethod = this.generator.getMethodTable().GetCastMethod(targetClass, sourceClass);
		this.generator.Debug("C1="+targetClass.getSimpleName()+ ", C2="+sourceClass.getSimpleName()+", CastMethod="+sMethod);
		if(sMethod != null) {
			String owner = Type.getInternalName(sMethod.getDeclaringClass());
			this.visitMethodInsn(INVOKESTATIC, owner, sMethod.getName(), Type.getMethodDescriptor(sMethod));
			this.checkCast(targetClass, sMethod.getReturnType());
		}
		else if (!targetClass.isAssignableFrom(sourceClass)) {
			// c1 instanceof C2  C2.
			this.generator.Debug("CHECKCAST C1="+targetClass.getSimpleName()+ ", given C2="+sourceClass.getSimpleName());
			this.visitTypeInsn(CHECKCAST, Type.getInternalName(targetClass));
		}
	}

	void checkParamCast(Class<?> targetClass, BNode node) {
		Class<?> sourceClass = this.generator.GetJavaClass(node.Type);
		if(targetClass != sourceClass) {
			this.generator.Debug("C2="+node + ": " + node.Type);
			this.checkCast(targetClass, sourceClass);
		}
	}

	void checkReturnCast(BNode node, Class<?> sourceClass) {
		Class<?> targetClass = this.generator.GetJavaClass(node.Type);
		if(targetClass != sourceClass) {
			this.generator.Debug("C1 "+node + ": " + node.Type);
			this.checkCast(targetClass, sourceClass);
		}
	}

	void pushNode(Class<?> targetClass, BNode node) {
		node.Accept(this.generator);
		if(targetClass != null) {
			this.checkParamCast(targetClass, node);
		}
	}

	void applyStaticMethod(BNode node, Method sMethod) {
		String owner = Type.getInternalName(sMethod.getDeclaringClass());
		this.setLineNumber(node);
		this.visitMethodInsn(INVOKESTATIC, owner, sMethod.getName(), Type.getMethodDescriptor(sMethod));
		this.checkReturnCast(node, sMethod.getReturnType());
	}

	void applyStaticMethod(BNode node, Method sMethod, BNode[] nodes) {
		Class<?>[] paramClasses = sMethod.getParameterTypes();
		for(int i = 0; i < paramClasses.length; i++) {
			this.pushNode(paramClasses[i], nodes[i]);
		}
		this.applyStaticMethod(node, sMethod);
	}

	void applyStaticMethod(BNode node, Method sMethod, AbstractListNode listNode) {
		Class<?>[] paramClasses = sMethod.getParameterTypes();
		for(int i = 0; i < paramClasses.length; i++) {
			this.pushNode(paramClasses[i], listNode.GetListAt(i));
		}
		this.applyStaticMethod(node, sMethod);
	}

	void applyFuncName(BNode node, String funcName, BFuncType funcType, AbstractListNode listNode) {
		if(listNode != null) {
			for(int i = 0; i < listNode.GetListSize(); i++) {
				this.pushNode(null, listNode.GetListAt(i));
			}
		}
		this.setLineNumber(node);
		Class<?> funcClass = this.generator.GetDefinedFunctionClass(funcName, funcType);
		if(funcClass != null) {
			this.visitMethodInsn(INVOKESTATIC, funcClass, "f", funcType);
		}
		else {
			// in some case, class has not been generated
			this.generator.LazyBuild(funcType.StringfySignature(funcName));
			this.visitMethodInsn(INVOKESTATIC, this.generator.NameFunctionClass(funcName, funcType), "f", funcType);
		}
	}

	void applyFuncObject(BNode node, Class<?> funcClass, BNode funcNode, BFuncType funcType, AbstractListNode listNode) {
		this.pushNode(funcClass, funcNode);
		for(int i = 0; i < listNode.GetListSize(); i++) {
			this.pushNode(null, listNode.GetListAt(i));
		}
		this.setLineNumber(node);
		this.visitMethodInsn(INVOKEVIRTUAL, funcClass, "Invoke", funcType);
	}

	void oushNodeListAsArray(Class<?> T, int startIndex, AbstractListNode nodeList) {
		this.pushInt(nodeList.GetListSize() - startIndex);
		int storeOpcode = -1;
		if(T == boolean.class) {
			this.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);
			storeOpcode = BASTORE;
		}
		else if(T == long.class) {
			this.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_LONG);
			storeOpcode = LASTORE;
		}
		else if(T == double.class) {
			this.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_DOUBLE);
			storeOpcode = DASTORE;
		}
		else {
			this.visitTypeInsn(ANEWARRAY, Type.getInternalName(T));
			storeOpcode = AASTORE;
		}
		for(int i = startIndex; i < nodeList.GetListSize() ; i++) {
			this.visitInsn(DUP);
			this.pushInt(i - startIndex);
			this.pushNode(T, nodeList.GetListAt(i));
			this.visitInsn(storeOpcode);
		}
	}

	public void visitReturn(BType returnType) {
		if(returnType.IsVoidType()) {
			this.visitInsn(RETURN);
		}
		else {
			Type type = this.generator.javaTypeUtils.AsmType(returnType);
			this.visitInsn(type.getOpcode(IRETURN));
		}
	}

	public void visitMethodInsn(int acc, String className, String funcName, BFuncType funcType) {
		this.visitMethodInsn(acc, className, funcName, this.generator.javaTypeUtils.GetMethodDescriptor(funcType));
	}

	public void visitMethodInsn(int acc, Class<?> jClass, String funcName, BFuncType funcType) {
		this.visitMethodInsn(acc, Type.getInternalName(jClass), funcName, this.generator.javaTypeUtils.GetMethodDescriptor(funcType));
	}

	public void visitFieldInsn(int opcode, String className, String name, Class<?> jClass) {
		this.visitFieldInsn(opcode, className, name, Type.getDescriptor(jClass));
	}

	public void visitTypeInsn(int opcode, Class<?> C) {
		this.visitTypeInsn(opcode, Type.getInternalName(C));
	}
}
