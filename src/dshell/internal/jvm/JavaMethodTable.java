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
import java.lang.reflect.Method;
import java.util.HashMap;

import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.util.LibBunSystem;

public class JavaMethodTable {
	protected final JavaTypeTable typeTable;
	protected final HashMap<String, Method> MethodMap = new HashMap<String,Method>();

	public JavaMethodTable(JavaTypeTable typeTable) {
		this.typeTable = typeTable;
		
		Import("!", BType.VarType, JavaOperatorApi.class, "Not");
		Import("+", BType.VarType, JavaOperatorApi.class, "Plus");
		Import("-", BType.VarType, JavaOperatorApi.class, "Minus");
		Import("~", BType.VarType, JavaOperatorApi.class, "BitwiseNot");
		Import(BType.VarType, "+", BType.VarType, JavaOperatorApi.class, "Add");
		Import(BType.VarType, "-", BType.VarType, JavaOperatorApi.class, "Sub");
		Import(BType.VarType, "*", BType.VarType, JavaOperatorApi.class, "Mul");
		Import(BType.VarType, "/", BType.VarType, JavaOperatorApi.class, "Div");
		Import(BType.VarType, "%", BType.VarType, JavaOperatorApi.class, "Mod");
		Import(BType.VarType, "==", BType.VarType, JavaOperatorApi.class, "Equals");
		Import(BType.VarType, "!=", BType.VarType, JavaOperatorApi.class, "NotEquals");
		Import(BType.VarType, "<", BType.VarType, JavaOperatorApi.class, "LessThan");
		Import(BType.VarType, "<=", BType.VarType, JavaOperatorApi.class, "LessThanEquals");
		Import(BType.VarType, ">", BType.VarType, JavaOperatorApi.class, "GreaterThan");
		Import(BType.VarType, ">=", BType.VarType, JavaOperatorApi.class, "GreaterThanEquals");
		Import(BType.VarType, "&", BType.VarType, JavaOperatorApi.class, "BitwiseAnd");
		Import(BType.VarType, "|", BType.VarType, JavaOperatorApi.class, "BitwiseOr");
		Import(BType.VarType, "^", BType.VarType, JavaOperatorApi.class, "BitwiseXor");
		Import(BType.VarType, "<<", BType.VarType, JavaOperatorApi.class, "LeftShift");
		Import(BType.VarType, ">>", BType.VarType, JavaOperatorApi.class, "RightShift");

		Import("!", BType.BooleanType, JavaOperatorApi.class, "Not");
		Import("+", BType.IntType, JavaOperatorApi.class, "Plus");
		Import("-", BType.IntType, JavaOperatorApi.class, "Minus");
		Import("~", BType.IntType, JavaOperatorApi.class, "BitwiseNot");
		Import(BType.IntType, "+", BType.IntType, JavaOperatorApi.class, "Add");
		Import(BType.IntType, "-", BType.IntType, JavaOperatorApi.class, "Sub");
		Import(BType.IntType, "*", BType.IntType, JavaOperatorApi.class, "Mul");
		Import(BType.IntType, "/", BType.IntType, JavaOperatorApi.class, "Div");
		Import(BType.IntType, "%", BType.IntType, JavaOperatorApi.class, "Mod");
		Import(BType.IntType, "==", BType.IntType, JavaOperatorApi.class, "Equals");
		Import(BType.IntType, "!=", BType.IntType, JavaOperatorApi.class, "NotEquals");
		Import(BType.IntType, "<", BType.IntType, JavaOperatorApi.class, "LessThan");
		Import(BType.IntType, "<=", BType.IntType, JavaOperatorApi.class, "LessThanEquals");
		Import(BType.IntType, ">", BType.IntType, JavaOperatorApi.class, "GreaterThan");
		Import(BType.IntType, ">=", BType.IntType, JavaOperatorApi.class, "GreaterThanEquals");
		Import(BType.IntType, "&", BType.IntType, JavaOperatorApi.class, "BitwiseAnd");
		Import(BType.IntType, "|", BType.IntType, JavaOperatorApi.class, "BitwiseOr");
		Import(BType.IntType, "^", BType.IntType, JavaOperatorApi.class, "BitwiseXor");
		Import(BType.IntType, "<<", BType.IntType, JavaOperatorApi.class, "LeftShift");
		Import(BType.IntType, ">>", BType.IntType, JavaOperatorApi.class, "RightShift");

		Import("+", BType.FloatType, JavaOperatorApi.class, "Plus");
		Import("-", BType.FloatType, JavaOperatorApi.class, "Minus");
		Import(BType.FloatType, "+", BType.FloatType, JavaOperatorApi.class, "Add");
		Import(BType.FloatType, "-", BType.FloatType, JavaOperatorApi.class, "Sub");
		Import(BType.FloatType, "*", BType.FloatType, JavaOperatorApi.class, "Mul");
		Import(BType.FloatType, "/", BType.FloatType, JavaOperatorApi.class, "Div");
		Import(BType.FloatType, "%", BType.FloatType, JavaOperatorApi.class, "Mod");
		Import(BType.FloatType, "==", BType.FloatType, JavaOperatorApi.class, "Equals");
		Import(BType.FloatType, "!=", BType.FloatType, JavaOperatorApi.class, "NotEquals");
		Import(BType.FloatType, "<", BType.FloatType, JavaOperatorApi.class, "LessThan");
		Import(BType.FloatType, "<=", BType.FloatType, JavaOperatorApi.class, "LessThanEquals");
		Import(BType.FloatType, ">", BType.FloatType, JavaOperatorApi.class, "GreaterThan");
		Import(BType.FloatType, ">=", BType.FloatType, JavaOperatorApi.class, "GreaterThanEquals");

		Import(BType.StringType, "+", BType.StringType, JavaOperatorApi.class, "Add");
		Import(BType.StringType, "==", BType.StringType, JavaOperatorApi.class, "Equals");
		Import(BType.StringType, "!=", BType.StringType, JavaOperatorApi.class, "NotEquals");
		Import(BType.StringType, "[]", BType.IntType, JavaOperatorApi.class, "GetIndex");

		BType BooleanArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.BooleanType);
		BType IntArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.IntType);
		BType FloatArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.FloatType);

		Import(BGenericType._ArrayType, "[]", BType.IntType, libbun.util.BArray.class, "GetIndex");
		Import(BGenericType._ArrayType, "[]=", BType.IntType, libbun.util.BArray.class, "SetIndex", Object.class);
		Import(BooleanArrayType, "[]", BType.IntType, libbun.util.BBooleanArray.class, "GetIndex");
		Import(BooleanArrayType, "[]=", BType.IntType, libbun.util.BBooleanArray.class, "SetIndex", boolean.class);
		Import(IntArrayType, "[]", BType.IntType, libbun.util.BIntArray.class, "GetIndex");
		Import(IntArrayType, "[]=", BType.IntType, libbun.util.BIntArray.class, "SetIndex", long.class);
		Import(FloatArrayType, "[]", BType.IntType, libbun.util.BFloatArray.class, "GetIndex");
		Import(FloatArrayType, "[]=", BType.IntType, libbun.util.BFloatArray.class, "SetIndex", double.class);

		Import(BGenericType._MapType, "[]", BType.StringType, libbun.util.BunMap.class, "GetIndex");
		Import(BGenericType._MapType, "[]=", BType.StringType, libbun.util.BunMap.class, "SetIndex", Object.class);


		Import(boolean.class, JavaCastApi.class, "toObject");
		Import(byte.class, JavaCastApi.class, "toObject");
		Import(short.class, JavaCastApi.class, "toObject");
		Import(int.class, JavaCastApi.class, "toObject");
		Import(long.class, JavaCastApi.class, "toObject");
		Import(float.class, JavaCastApi.class, "toObject");
		Import(double.class, JavaCastApi.class, "toObject");

		Import(Object.class, JavaCastApi.class, "toboolean");
		Import(Boolean.class, JavaCastApi.class, "toboolean");
		Import(Object.class, JavaCastApi.class, "tobyte");
		Import(long.class, JavaCastApi.class, "tobyte");
		Import(Object.class, JavaCastApi.class, "toshort");
		Import(long.class, JavaCastApi.class, "toshort");
		Import(Object.class, JavaCastApi.class, "toint");
		Import(long.class, JavaCastApi.class, "toint");
		Import(Object.class, JavaCastApi.class, "tolong");
		Import(byte.class, JavaCastApi.class,  "tolong");
		Import(short.class, JavaCastApi.class, "tolong");
		Import(int.class, JavaCastApi.class, "tolong");
		Import(double.class, JavaCastApi.class, "tolong");
		Import(Byte.class, JavaCastApi.class,  "tolong");
		Import(Short.class, JavaCastApi.class, "tolong");
		Import(Integer.class, JavaCastApi.class, "tolong");
		Import(Long.class, JavaCastApi.class, "tolong");
		Import(Object.class, JavaCastApi.class, "tofloat");
		Import(double.class, JavaCastApi.class, "tofloat");
		Import(Object.class, JavaCastApi.class, "todouble");
		Import(long.class, JavaCastApi.class, "todouble");
		Import(float.class, JavaCastApi.class, "todouble");
		Import(Float.class, JavaCastApi.class, "todouble");
		Import(Double.class, JavaCastApi.class, "todouble");

		Import(Object.class, JavaCastApi.class, "toBoolean");
		Import(boolean.class, JavaCastApi.class, "toBoolean");
		Import(Object.class, JavaCastApi.class, "toByte");
		Import(long.class, JavaCastApi.class, "toByte");
		Import(Object.class, JavaCastApi.class, "toShort");
		Import(long.class, JavaCastApi.class, "toShort");
		Import(Object.class, JavaCastApi.class, "toInteger");
		Import(long.class, JavaCastApi.class, "toInteger");
		Import(Object.class, JavaCastApi.class, "toLong");
		Import(byte.class, JavaCastApi.class,  "toLong");
		Import(short.class, JavaCastApi.class, "toLong");
		Import(int.class, JavaCastApi.class, "toLong");
		Import(long.class, JavaCastApi.class, "toLong");
		Import(Object.class, JavaCastApi.class, "toFloat");
		Import(double.class, JavaCastApi.class, "toFloat");
		Import(Object.class, JavaCastApi.class, "toDouble");
		Import(double.class, JavaCastApi.class, "toDouble");

		Import(boolean.class, JavaCastApi.class, "toString");
		Import(Boolean.class, JavaCastApi.class, "toString");
		Import(long.class, JavaCastApi.class, "toString");
		Import(Long.class, JavaCastApi.class, "toString");
		Import(double.class, JavaCastApi.class, "toString");
		Import(Double.class, JavaCastApi.class, "toString");
	}

	public void Import(Class<?> BaseClass, String Name, Class<?> ... T1) {
		try {
			Method sMethod = BaseClass.getMethod(Name, T1);
			MethodMap.put(Name, sMethod);
		} catch (Exception e) {
			System.err.println("FIXME:" + e);
		}
	}

	public String BinaryKey(BType T1, String Op, BType T2) {
		return T1.GetUniqueName()+Op+T2.GetUniqueName();
	}
	public String UnaryKey(String Op, BType T2) {
		return Op+T2.GetUniqueName();
	}
	public String CastKey(Class<?> T1, Class<?> T2) {
		return T1.getCanonicalName() + ":" + T2.getCanonicalName();
	}
	public void Import(BType T1, String Op, BType T2, Class<?> BaseClass, String Name) {
		try {
			Method sMethod = BaseClass.getMethod(Name, this.typeTable.GetJavaClass(T1, null), this.typeTable.GetJavaClass(T2, null));
			MethodMap.put(BinaryKey(T1, Op, T2), sMethod);
		} catch (Exception e) {
			System.err.println("FIXME:" + e);
		}
	}
	public void Import(BType T1, String Op, BType T2, Class<?> BaseClass, String Name, Class<?> T3) {
		try {
			Method sMethod = BaseClass.getMethod(Name, this.typeTable.GetJavaClass(T1, null), this.typeTable.GetJavaClass(T2, null), T3);
			MethodMap.put(BinaryKey(T1, Op, T2), sMethod);
		} catch (Exception e) {
			System.err.println("FIXME:" + e);
		}
	}
	public void Import(String Op, BType T1, Class<?> BaseClass, String Name) {
		try {
			Method sMethod = BaseClass.getMethod(Name, this.typeTable.GetJavaClass(T1, null));
			MethodMap.put(UnaryKey(Op, T1), sMethod);
		} catch (Exception e) {
			System.err.println("FIXME:" + e);
		}
	}
	public void Import(Class<?> T1, Class<?> BaseClass, String Name) {
		try {
			Method sMethod = BaseClass.getMethod(Name, T1);
			MethodMap.put(CastKey(sMethod.getReturnType(), T1), sMethod);
		} catch (Exception e) {
			System.err.println("FIXME:" + e);
		}
	}

	public Method GetStaticMethod(String Name) {
		return MethodMap.get(Name);
	}

	public Method GetBinaryStaticMethod(BType T1, String Op, BType T2) {
		Method sMethod = MethodMap.get(BinaryKey(T1, Op, T2));
		while(sMethod == null) {
			LibBunSystem._PrintDebug("unfound binary operator" + T1 + " " + Op + " " + T2);
			if(T1.IsVarType()) {
				sMethod = MethodMap.get(BinaryKey(BType.VarType, Op, BType.VarType));
				break;
			}
			T1 = T1.GetSuperType();
			sMethod = MethodMap.get(BinaryKey(T1, Op, T2));
		}
		return sMethod;
	}

	public Method GetUnaryStaticMethod(String Op, BType T2) {
		Method sMethod = MethodMap.get(UnaryKey(Op, T2));
		if(sMethod == null) {
			// if undefined Object "op" Object must be default
			sMethod = MethodMap.get(UnaryKey(Op, BType.VarType));
		}
		return sMethod;
	}

	public Method GetCastMethod(Class<?> T1, Class<?> T2) {
		Method sMethod = MethodMap.get(CastKey(T1, T2));
		if(sMethod == null) {
			// if undefined Object "op" Object must be default
			sMethod = MethodMap.get(CastKey(Object.class, T2));
		}
		return sMethod;
	}
}
