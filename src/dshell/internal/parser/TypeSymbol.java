package dshell.internal.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import dshell.internal.parser.error.TypeLookupException;
import dshell.internal.type.TypePool;
import dshell.internal.type.DSType;

/**
 * contains parsed type symbol.
 * @author skgchxngsxyz-opensuse
 *
 */
public abstract class TypeSymbol {
	/**
	 * represent type token.
	 */
	protected final Token token;

	protected TypeSymbol(Token token) {
		this.token = token;
	}

	public Token getToken() {
		return this.token;
	}

	/**
	 * TypeSymbol to Type.
	 * called from TypeChecker
	 * @param pool
	 * @return
	 * - throw exception, if type not found.
	 */
	public abstract DSType toType(TypePool pool);

	public static TypeSymbol toPrimitive(Token token) {
		return new PrimitiveTypeSymbol(token);
	}

	public static TypeSymbol toVoid(Token token) {
		return new VoidTypeSymbol(token);
	}

	public static TypeSymbol toVoid() {
		return new VoidTypeSymbol(null);
	}

	public static TypeSymbol toClass(Token token) {
		return new ClassTypeSymbol(token);
	}

	public static TypeSymbol toFunc(Token token, TypeSymbol returnTypeSymbol, TypeSymbol[] paramTypeSymbols) {
		return new FuncTypeSymbol(token, returnTypeSymbol, paramTypeSymbols);
	}

	public static TypeSymbol toGeneric(Token token, TypeSymbol[] typeSymbols) {
		return new GenericTypeSymbol(token, typeSymbols);
	}

	public static class PrimitiveTypeSymbol extends TypeSymbol {
		private PrimitiveTypeSymbol(Token token) {
			super(token);
		}

		@Override
		public DSType toType(TypePool pool) {
			try {
				return pool.getPrimitiveType(this.token.getText());
			} catch(TypeLookupException e) {
				TypeLookupException.formateAndPropagateException(e, this.token);
			}
			return null;
		}
	}

	public static class VoidTypeSymbol extends TypeSymbol {
		private VoidTypeSymbol(Token token) {
			super(token);
		}

		@Override
		public DSType toType(TypePool pool) {
			return TypePool.voidType;
		}
	}

	public static class ClassTypeSymbol extends TypeSymbol {
		private ClassTypeSymbol(Token token) {
			super(token);
		}

		@Override
		public DSType toType(TypePool pool) {
			try {
				return pool.getClassType(this.token.getText());
			} catch(TypeLookupException e) {
				TypeLookupException.formateAndPropagateException(e, this.token);
			}
			return null;
		}
	}

	public static class FuncTypeSymbol extends TypeSymbol {
		private final TypeSymbol returnTypeSymbol;
		private final TypeSymbol[] paramtypeSymbols;

		private FuncTypeSymbol(Token token, TypeSymbol returnTypeSymbol, TypeSymbol[] paramTypeSymbols) {
			super(token);
			this.returnTypeSymbol = returnTypeSymbol;
			this.paramtypeSymbols = paramTypeSymbols;
		}

		@Override
		public DSType toType(TypePool pool) {
			DSType returnType = this.returnTypeSymbol.toType(pool);
			List<DSType> paramTypeList = new ArrayList<>(this.paramtypeSymbols.length);
			for(TypeSymbol typeSymbol : this.paramtypeSymbols) {
				paramTypeList.add(typeSymbol.toType(pool));
			}
			try {
				return pool.createAndGetFuncTypeIfUndefined(returnType, paramTypeList);
			} catch(TypeLookupException e) {
				TypeLookupException.formateAndPropagateException(e, this.token);
			}
			return null;
		}
	}

	public static class GenericTypeSymbol extends TypeSymbol {
		private final TypeSymbol[] typeSymbols;

		private GenericTypeSymbol(Token token, TypeSymbol[] typeSymbols) {
			super(token);
			this.typeSymbols = typeSymbols;
		}

		@Override
		public DSType toType(TypePool pool) {
			List<DSType> elementTypeList = new ArrayList<>(this.typeSymbols.length);
			for(TypeSymbol typeSymbol : this.typeSymbols) {
				elementTypeList.add(typeSymbol.toType(pool));
			}
			try {
				return pool.createAndGetReifiedTypeIfUndefined(this.token.getText(), elementTypeList);
			} catch(TypeLookupException e) {
				TypeLookupException.formateAndPropagateException(e, this.token);
			}
			return null;
		}
	}
}
