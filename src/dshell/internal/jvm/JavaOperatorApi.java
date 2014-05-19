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

public final class JavaOperatorApi {
	public static boolean Not(Object x) {
		return ((Boolean)x).booleanValue();
	}

	public static Object Plus(Object x) {
		if(x instanceof Number) {
			return x;
		}
		throw new RuntimeException("unsupported operator + " + x.getClass().getSimpleName());
	}
	public static Object Minus(Object x) {
		if(x instanceof Double) {
			return -((Long)x).doubleValue();
		}
		return -((Number)x).longValue();
	}
	public static long BitwiseNot(Object x) {
		if(x instanceof Number && !(x instanceof Double)) {
			return ~((Number)x).longValue();
		}
		throw new RuntimeException("unsupported operator ~ " + x.getClass().getSimpleName());
	}
	public static Object Add(Object x, Object y) {
		if(x instanceof String || y instanceof String) {
			return "" + x + y;
		}
		if(x instanceof Double || y instanceof Double) {
			return ((Number)x).doubleValue() + ((Number)y).doubleValue();
		}
		return ((Number)x).longValue() + ((Number)y).longValue();
	}
	public static Object Sub(Object x, Object y) {
		if(x instanceof Double || y instanceof Double) {
			return ((Double)x).doubleValue() - ((Double)y).doubleValue();
		}
		return ((Number)x).longValue() - ((Number)y).longValue();
	}
	public static Object Mul(Object x, Object y) {
		if(x instanceof Double || y instanceof Double) {
			return ((Number)x).doubleValue() * ((Number)y).doubleValue();
		}
		return ((Number)x).longValue() * ((Number)y).longValue();
	}
	public static Object Div(Object x, Object y) {
		if(x instanceof Double || y instanceof Double) {
			return ((Number)x).doubleValue() / ((Number)y).doubleValue();
		}
		return ((Number)x).longValue() / ((Number)y).longValue();
	}
	public static Object Mod(Object x, Object y) {
		if(x instanceof Double || y instanceof Double) {
			return ((Number)x).doubleValue() % ((Number)y).doubleValue();
		}
		return ((Number)x).longValue() / ((Number)y).longValue();
	}
	public static long LeftShift(Object x, Object y) {
		return ((Number)x).longValue() << ((Number)y).longValue();
	}
	public static long RightShift(Object x, Object y) {
		return ((Number)x).longValue() >> ((Number)y).longValue();
	}
	public static long BitwiseAnd(Object x, Object y) {
		return ((Number)x).longValue() & ((Number)y).longValue();
	}
	public static long BitwiseOr(Object x, Object y) {
		return ((Number)x).longValue() | ((Number)y).longValue();
	}
	public static long BitwiseXor(Object x, Object y) {
		return ((Number)x).longValue() ^ ((Number)y).longValue();
	}
	public static boolean LessThan(Object x, Object y) {
		return ((Number)x).doubleValue() < ((Number)y).doubleValue();
	}
	public static boolean LessThanEquals(Object x, Object y) {
		return ((Number)x).doubleValue() <= ((Number)y).doubleValue();
	}
	public static boolean GreaterThan(Object x, Object y) {
		return ((Number)x).doubleValue() > ((Number)y).doubleValue();
	}
	public static boolean GreaterThanEquals(Object x, Object y) {
		return ((Number)x).doubleValue() >= ((Number)y).doubleValue();
	}
	public static boolean Equals(Object x, Object y) {
		if(x instanceof Number && y instanceof Number) {
			return ((Number)x).doubleValue() == ((Number)y).doubleValue();
		}
		return x == y;
	}
	public static boolean NotEquals(Object x, Object y) {
		if(x instanceof Number && y instanceof Number) {
			return ((Number)x).doubleValue() != ((Number)y).doubleValue();
		}
		return x != y;
	}

	public static boolean Not(boolean b) {
		return !b;
	}
	public static boolean Equals(boolean x, boolean y) {
		return x == y;
	}
	public static boolean NotEquals(boolean x, boolean y) {
		return x != y;
	}
	//
	public static long Plus(long n) {
		return +n;
	}
	public static long Minus(long n) {
		return -n;
	}
	public static long BitwiseNot(long n) {
		return ~n;
	}
	public static long Add(long x, long y) {
		return x + y;
	}
	public static long Sub(long x, long y) {
		return x - y;
	}
	public static long Mul(long x, long y) {
		return x * y;
	}
	public static long Div(long x, long y) {
		return x / y;
	}
	public static long Mod(long x, long y) {
		return x % y;
	}
	public static long LeftShift(long x, long y) {
		return x << y;
	}
	public static long RightShift(long x, long y) {
		return x >> y;
	}
	public static long BitwiseAnd(long x, long y) {
		return x & y;
	}
	public static long BitwiseOr(long x, long y) {
		return x | y;
	}
	public static long BitwiseXor(long x, long y) {
		return x ^ y;
	}
	public static boolean LessThan(long x, long y) {
		return x < y;
	}
	public static boolean LessThanEquals(long x, long y) {
		return x <= y;
	}
	public static boolean GreaterThan(long x, long y) {
		return x > y;
	}
	public static boolean GreaterThanEquals(long x, long y) {
		return x >= y;
	}
	public static boolean Equals(long x, long y) {
		return x == y;
	}
	public static boolean NotEquals(long x, long y) {
		return x != y;
	}
	//
	public static double Plus(double n) {
		return +n;
	}
	public static double Minus(double n) {
		return -n;
	}
	public static double Add(double x, double y) {
		return x + y;
	}
	public static double Sub(double x, double y) {
		return x - y;
	}
	public static double Mul(double x, double y) {
		return x * y;
	}
	public static double Div(double x, double y) {
		return x / y;
	}
	public static double Mod(double x, double y) {
		return x % y;
	}
	public static boolean LessThan(double x, double y) {
		return x < y;
	}
	public static boolean LessThanEquals(double x, double y) {
		return x <= y;
	}
	public static boolean GreaterThan(double x, double y) {
		return x > y;
	}
	public static boolean GreaterThanEquals(double x, double y) {
		return x >= y;
	}
	public static boolean Equals(double x, double y) {
		return x == y;
	}
	public static boolean NotEquals(double x, double y) {
		return x != y;
	}
	//
	public static String Add(String x, String y) {
		return x + y;
	}
	public static boolean Equals(String x, String y) {
		if(x == null || y == null) {
			return x == y;
		}
		else {
			return x.equals(y);
		}
	}
	public static boolean NotEquals(String x, String y) {
		return !Equals(x, y);
	}

	public static String GetIndex(String x, long y) {
		return String.valueOf(x.charAt((int)y));
	}
}
