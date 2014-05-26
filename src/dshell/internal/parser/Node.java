package dshell.internal.parser;

import dshell.internal.parser.TypePool.Type;

public class Node {
	// ##################
	// #   expression   #
	// ##################
	/**
	 * This node represents expression.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static abstract class ExprNode extends Node {
		/**
		 * If true, this node is considered as statement.
		 * Evaluating after statement, jvm operand stack is empty.
		 */
		protected boolean asStatement = false;

		/**
		 * It represents expression return type.
		 */
		protected Type type = TypePool.getInstance().unresolvedType;

		/**
		 * Consider this node as statement.
		 */
		public final void requireStatement() {
			this.asStatement = true;
		}

		/**
		 * If true, this node is considered as statement.
		 * @return
		 */
		public final boolean isStatement() {
			return this.asStatement;
		}

		/**
		 * Set evaluated value type of this node.
		 * @param type
		 * - this node's evaluated value type
		 */
		public void setType(Type type) {
			this.type = type;
		}

		/**
		 * Get this node's evaluated value type
		 * @return
		 */
		public Type getType() {
			return this.type;
		}
	}

	/**
	 * This node represents int, float, boolean, String or null value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ConstValueNode extends ExprNode {
		
	}

	/**
	 * This node represents array literal.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ArrayNode extends ExprNode {
		
	}

	/**
	 * This node represents map literal.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class MapNode extends ExprNode {
		
	}

	/**
	 * This node represents local variable.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class SymbolNode extends ExprNode {
		
	}

	/**
	 * This node represents getting array element.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ElementGetterNode extends ExprNode {
		
	}

	/**
	 * This node represents getting class field.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FieldGetterNode extends ExprNode {
		
	}

	/**
	 * This node represents type cast.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class CastNode extends ExprNode {
		
	}

	/**
	 * This node represents suffix increment ('++', '--').
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class SuffixIncrementNode extends ExprNode {
		
	}

	/**
	 * This node represents function call.
	 * Function includes binary op, prefix op, or user defined function.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FuncCallNode extends ExprNode {
		
	}

	/**
	 * This node represents instance method call.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class MethodCallNode extends ExprNode {
		
	}

	/**
	 * This node represents class constructor call.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ConstructorCallNode extends ExprNode {
		
	}

	/**
	 * This node represents conditional and operator.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class AndNode extends ExprNode {
		
	}

	/**
	 * This node represents conditional or operator.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class OrNode extends ExprNode {
		
	}

	//TODO: adding shell command represent nodes

	// #################
	// #   statement   #
	// #################
	/**
	 * This node represents assertion.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class AssertNode extends Node {
		
	}

	/**
	 * This node represents block.
	 * It contains several statements.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class BlockNode extends Node {
		
	}

	/**
	 * This node represents break statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class BreakNode extends Node {
		
	}

	/**
	 * This node represents continue statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ContinueNode extends Node {
		
	}

	/**
	 * This node represents exporting variable as environmental variable.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ExportEnvNode extends Node {
		
	}

	/**
	 * This node represents importing environmental variable.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ImportEnvNode extends Node {
		
	}

	/**
	 * This node represents for statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ForNode extends Node {
		
	}

	/**
	 * This node represents for in statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ForInNode extends Node {
		
	}

	/**
	 * This node represents while statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class WhileNode extends Node {
		
	}

	/**
	 * This node represents if statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class IfNode extends Node {
		
	}

	/**
	 * This node represents return statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ReturnNode extends Node {
		
	}

	/**
	 * This node represents throw statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ThrowNode extends Node {
		
	}

	/**
	 * This node represents try-catch statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class TryNode extends Node {
		
	}

	/**
	 * This node represents catch statement.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class CatchNode extends Node {
		
	}

	/**
	 * This node represents variable declaration.
	 * It requires initial value.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class VarDeclNode extends Node {
		
	}

	/**
	 * This node represents assign operation such as '=', '+=', '-=', '*=', '/=', '%='.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class AssignNode extends Node {
		
	}

	/**
	 * This node represents function.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class FunctionNode extends Node {
		
	}

	/**
	 * This node represents class.
	 * It contains class field, instance method, constructor.
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class ClassNode extends Node {
		
	}
}
