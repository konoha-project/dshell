// Generated from dshell.g4 by ANTLR 4.2

package dshell.internal.parser;
import dshell.internal.parser.TypePool;
import dshell.internal.parser.Node;
import dshell.internal.parser.NodeUtils;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link dshellParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface dshellVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link dshellParser#symbol}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbol(@NotNull dshellParser.SymbolContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(@NotNull dshellParser.ArgumentContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#arrayLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayLiteral(@NotNull dshellParser.ArrayLiteralContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#forCond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForCond(@NotNull dshellParser.ForCondContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(@NotNull dshellParser.TypeNameContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#toplevelStatements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToplevelStatements(@NotNull dshellParser.ToplevelStatementsContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#returnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(@NotNull dshellParser.ReturnStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#finallyBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinallyBlock(@NotNull dshellParser.FinallyBlockContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#tryCatchStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTryCatchStatement(@NotNull dshellParser.TryCatchStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#mapEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapEntry(@NotNull dshellParser.MapEntryContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#classDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDeclaration(@NotNull dshellParser.ClassDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#forIter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForIter(@NotNull dshellParser.ForIterContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(@NotNull dshellParser.LiteralContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#toplevel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToplevel(@NotNull dshellParser.ToplevelContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(@NotNull dshellParser.StatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#toplevelStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToplevelStatement(@NotNull dshellParser.ToplevelStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(@NotNull dshellParser.BlockContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#blockStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockStatement(@NotNull dshellParser.BlockStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#classBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBlock(@NotNull dshellParser.ClassBlockContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(@NotNull dshellParser.ExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#throwStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrowStatement(@NotNull dshellParser.ThrowStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#classElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassElement(@NotNull dshellParser.ClassElementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#importCommandStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportCommandStatement(@NotNull dshellParser.ImportCommandStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#argumentsDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentsDeclaration(@NotNull dshellParser.ArgumentsDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDeclaration(@NotNull dshellParser.FieldDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#breakStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStatement(@NotNull dshellParser.BreakStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#forStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(@NotNull dshellParser.ForStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(@NotNull dshellParser.IfStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorDeclaration(@NotNull dshellParser.ConstructorDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#catchStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchStatement(@NotNull dshellParser.CatchStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#importEnvStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportEnvStatement(@NotNull dshellParser.ImportEnvStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(@NotNull dshellParser.VariableDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(@NotNull dshellParser.WhileStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#foreachStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeachStatement(@NotNull dshellParser.ForeachStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#exceptDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExceptDeclaration(@NotNull dshellParser.ExceptDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#assertStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssertStatement(@NotNull dshellParser.AssertStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#forInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInit(@NotNull dshellParser.ForInitContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#exportEnvStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExportEnvStatement(@NotNull dshellParser.ExportEnvStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#assignStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignStatement(@NotNull dshellParser.AssignStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#continueStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStatement(@NotNull dshellParser.ContinueStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(@NotNull dshellParser.ArgumentsContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#variableDeclarationWithType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarationWithType(@NotNull dshellParser.VariableDeclarationWithTypeContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#mapLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapLiteral(@NotNull dshellParser.MapLiteralContext ctx);

	/**
	 * Visit a parse tree produced by {@link dshellParser#functionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclaration(@NotNull dshellParser.FunctionDeclarationContext ctx);
}