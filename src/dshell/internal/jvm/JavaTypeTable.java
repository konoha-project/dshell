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
import java.lang.reflect.Modifier;
import java.util.HashMap;

import libbun.type.BFuncType;
import libbun.type.BGenericType;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.util.BunMap;
import libbun.util.LibBunSystem;
import libbun.util.SoftwareFault;
import libbun.util.BArray;
import libbun.util.BBooleanArray;
import libbun.util.BFloatArray;
import libbun.util.BFunction;
import libbun.util.BIntArray;

public class JavaTypeTable {
	protected HashMap<String, Class<?>> ClassMap = new HashMap<String,Class<?>>();
	protected HashMap<String, BType> TypeMap = new HashMap<String,BType>();

	public JavaTypeTable() {
		SetTypeTable(BType.VarType, Object.class);
		SetTypeTable(BType.VoidType, void.class);
		SetTypeTable(BType.BooleanType, boolean.class);
		SetTypeTable(BType.IntType, long.class);
		SetTypeTable(BType.FloatType, double.class);
		SetTypeTable(BType.StringType, String.class);
		SetTypeTable(BFuncType._FuncType, BFunction.class);
		SetTypeTable(BGenericType._ArrayType, BArray.class);
		SetTypeTable(BGenericType._MapType, BunMap.class);

		BType BooleanArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.BooleanType);
		BType IntArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.IntType);
		BType FloatArrayType = BTypePool._GetGenericType1(BGenericType._ArrayType, BType.FloatType);
		SetTypeTable(BooleanArrayType, BBooleanArray.class);
		SetTypeTable(IntArrayType, BIntArray.class);
		SetTypeTable(FloatArrayType, BFloatArray.class);

		SetTypeTable(BType.BooleanType, Boolean.class);
		SetTypeTable(BType.IntType, Long.class);
		SetTypeTable(BType.FloatType, Double.class);
		SetTypeTable(BType.IntType, int.class);
		SetTypeTable(BType.IntType, Integer.class);
		SetTypeTable(BType.IntType, short.class);
		SetTypeTable(BType.IntType, Short.class);
		SetTypeTable(BType.IntType, byte.class);
		SetTypeTable(BType.IntType, Byte.class);
		SetTypeTable(BType.FloatType, float.class);
		SetTypeTable(BType.FloatType, Float.class);
		SetTypeTable(BType.StringType, char.class);
		SetTypeTable(BType.StringType, Character.class);

		GetBunType(SoftwareFault.class);
	}

	public void SetTypeTable(BType zType, Class<?> c) {
		if(this.ClassMap.get(zType.GetUniqueName()) == null) {
			this.ClassMap.put(zType.GetUniqueName(), c);
		}
		this.TypeMap.put(c.getCanonicalName(), zType);
	}

	public Class<?> GetJavaClass(BType zType, Class<?> Default) {
		Class<?> jClass = this.ClassMap.get(zType.GetUniqueName());
		if(jClass == null) {
			jClass = this.ClassMap.get(zType.GetBaseType().GetUniqueName());
			if(jClass == null) {
				jClass = Default;
			}
		}
		return jClass;
	}

	public BType GetBunType(Class<?> JavaClass) {
		BType NativeType = this.TypeMap.get(JavaClass.getCanonicalName());
		if (NativeType == null) {
			NativeType = new JavaType(this, JavaClass);
			this.SetTypeTable(NativeType, JavaClass);
		}
		return NativeType;
	}

	public final BFuncType ConvertToFuncType(Method JMethod) {
		Class<?>[] ParamTypes = JMethod.getParameterTypes();
		BArray<BType> TypeList = new BArray<BType>(new BType[LibBunSystem._Size(ParamTypes) + 2]);
		if (!Modifier.isStatic(JMethod.getModifiers())) {
			TypeList.add(this.GetBunType(JMethod.getDeclaringClass()));
		}
		if (ParamTypes != null) {
			for(int i = 0; i < ParamTypes.length; i++) {
				TypeList.add(this.GetBunType(ParamTypes[i]));
			}
		}
		TypeList.add(this.GetBunType(JMethod.getReturnType()));
		return BTypePool._LookupFuncType2(TypeList);
	}

	public final BFuncType FuncType(Class<?> ReturnT, Class<?> ... paramsT) {
		BArray<BType> TypeList = new BArray<BType>(new BType[10]);
		for(Class<?> C : paramsT) {
			TypeList.add(this.GetBunType(C));
		}
		TypeList.add(this.GetBunType(ReturnT));
		return BTypePool._LookupFuncType2(TypeList);
	}
}
