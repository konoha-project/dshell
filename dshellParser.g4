parser grammar dshellParser;

options { tokenVocab=dshellLexer; }


@header {
package dshell.internal.parser;
import dshell.internal.parser.Node;
import dshell.internal.parser.ParserUtils;
import dshell.internal.parser.TypeSymbol;
}

@members {
// parser entry point.
public ToplevelContext startParser() {
	this.getScope().popAllScope();
	return this.toplevel();
}

private boolean isLineEnd() {
	int lineEndIndex = this.getCurrentToken().getTokenIndex() - 1;
	Token lineEndToken = _input.get(lineEndIndex);
	if(lineEndToken.getChannel() != Lexer.HIDDEN) {
		return false;
	}
	int type = lineEndToken.getType();
	return type == LineEnd;
}

private boolean here(final int type) {
	int index = this.getCurrentToken().getTokenIndex() - 1;
	Token ahead = _input.get(index);
	return (ahead.getChannel() == Lexer.HIDDEN) && (ahead.getType() == type);
}

private void enterCmd() {
	((dshellLexer) _input.getTokenSource()).enterCmd();
}

private void exitCmd() {
	((dshellLexer)_input.getTokenSource()).exitCmd();
}

private CommandScope getScope() {
	return ((dshellLexer)_input.getTokenSource()).getScope();
}
}

// ######################
// #        parse       #
// ######################

// statement definition

toplevel returns [Node.RootNode node]
	: (a+=toplevelStatement)* EOF
	 {
	 	$node = new Node.RootNode(_input.get(0));
	 	for(int i = 0; i < $a.size(); i++) {
	 		$node.addNode($a.get(i).node);
	 	}
	 }
	;

toplevelStatement returns [Node node]
	: functionDeclaration {$node = $functionDeclaration.node;}
	| classDeclaration {$node = $classDeclaration.node;}
	| statement {$node = $statement.node;}
	;

statementEnd
	: EOF
	| CommandEnd
	| ';'
	| {isLineEnd()}?
	;

functionDeclaration returns [Node node]
	: Function Identifier '(' argumentsDeclaration ')' returnType block
		{$node = new Node.FunctionNode($Function, $Identifier, $returnType.type, $argumentsDeclaration.decl, $block.node);}
	;

returnType returns [TypeSymbol type]
	: ':' typeName { $type = $typeName.type;}
	| { $type = TypeSymbol.toVoid(); }
	;

argumentsDeclaration returns [ParserUtils.ArgsDecl decl]
	: a+=variableDeclarationWithType (',' a+=variableDeclarationWithType)*
		{
			$decl = new ParserUtils.ArgsDecl();
			for(int i = 0; i < $a.size(); i++) {
				$decl.addArgDecl($a.get(i).arg);
			}
		}
	| { $decl = new ParserUtils.ArgsDecl();}
	;

variableDeclarationWithType returns [ParserUtils.ArgDecl arg]
	: Identifier ':' typeName {$arg = new ParserUtils.ArgDecl($Identifier, $typeName.type);}
	;

typeName returns [TypeSymbol type] locals [TypeSymbol[] types]
	: Int {$type = TypeSymbol.toPrimitive($Int);}
	| Float {$type = TypeSymbol.toPrimitive($Float);}
	| Boolean {$type = TypeSymbol.toPrimitive($Boolean);}
	| Void {$type = TypeSymbol.toVoid($Void);}
	| Identifier {$type = TypeSymbol.toClass($Identifier);}
	| Func '<' aa=typeName paramTypes '>'
		{$type = TypeSymbol.toFunc($Func, $aa.type, $paramTypes.types);}
	| Identifier '<' a+=typeName (',' a+=typeName)* '>'
		{
			$types = new TypeSymbol[$a.size()];
			for(int i = 0; i < $types.length; i++) {
				$types[i] = $a.get(i).type;
			}
			$type = TypeSymbol.toGeneric($Identifier, $types);
		}
	;

paramTypes returns [TypeSymbol[] types] locals [ParserUtils.ParamTypeResolver resolver]
	: ',' '[' a+=typeName (',' a+=typeName)* ']'
		{
			$resolver = new ParserUtils.ParamTypeResolver();
			for(int i = 0; i < $a.size(); i++) {
				$resolver.addTypeSymbol($a.get(i).type);
			}
			$types = $resolver.getTypeSymbols();
		}
	| { $resolver = new ParserUtils.ParamTypeResolver(); $types = $resolver.getTypeSymbols();}
	;

block returns [Node node] locals [ParserUtils.Block blockModel]
	: {getScope().createNewScope();} '{' b+=statement+ '}' {getScope().removeCurrentScope();}
		{
			$blockModel = new ParserUtils.Block();
			for(int i = 0; i < $b.size(); i++) {
				$blockModel.addNode($b.get(i).node);
			}
			$node = new Node.BlockNode($blockModel);
		}
	;

classDeclaration returns [Node node] locals [String superName]
	: Class name=Identifier (Extends a+=Identifier)? classBody
		{
			$superName = null;
			if($a.size() == 1) {
				$superName = $a.get(0).getText();
			}
			$node = new Node.ClassNode($Class, $name, $superName, $classBody.body.getNodeList());
		}
	;

classBody returns [ParserUtils.ClassBody body]
	: '{' (a+=classElement statementEnd)+ '}'
		{
			$body = new ParserUtils.ClassBody();
			for(int i = 0; i < $a.size(); i++) {
				$body.addNode($a.get(i).node);
			}
		}
	;

classElement returns [Node node]
	: fieldDeclaration {$node = $fieldDeclaration.node;}
	| functionDeclaration {$node = $functionDeclaration.node;}
	| constructorDeclaration {$node = $constructorDeclaration.node;}
	;

fieldDeclaration returns [Node node]
	: variableDeclaration
	;

constructorDeclaration returns [Node node]
	: Constructor '(' argumentsDeclaration ')' block
		{$node = new Node.ConstructorNode($Constructor, $argumentsDeclaration.decl, $block.node);}
	;

statement returns [Node node]
	: assertStatement statementEnd {$node = $assertStatement.node;}
	| emptyStatement {$node = $emptyStatement.node;}
	| breakStatement statementEnd {$node = $breakStatement.node;}
	| continueStatement statementEnd {$node = $continueStatement.node;}
	| exportEnvStatement statementEnd {$node = $exportEnvStatement.node;}
	| forStatement {$node = $forStatement.node;}
	| foreachStatement {$node = $foreachStatement.node;}
	| ifStatement {$node = $ifStatement.node;}
	| importEnvStatement statementEnd {$node = $importEnvStatement.node;}
	| importCommandStatement importCommandEnd {$node = $importCommandStatement.node;}
	| returnStatement statementEnd {$node = $returnStatement.node;}
	| throwStatement statementEnd {$node = $throwStatement.node;}
	| whileStatement {$node = $whileStatement.node;}
	| tryCatchStatement {$node = $tryCatchStatement.node;}
	| variableDeclaration statementEnd {$node = $variableDeclaration.node;}
	| assignStatement statementEnd {$node = $assignStatement.node;}
	| suffixStatement statementEnd {$node = $suffixStatement.node;}
	| expression statementEnd {$node = $expression.node;}
	| commandExpression statementEnd {$node = $commandExpression.node;}
	;

assertStatement returns [Node node]
	: Assert '(' expression ')' {$node = new Node.AssertNode($Assert, $expression.node);}
	;

breakStatement returns [Node node]
	: Break {$node = new Node.BreakNode($Break);}
	;

continueStatement returns [Node node]
	: Continue {$node = new Node.ContinueNode($Continue);}
	;

exportEnvStatement returns [Node node]
	: ExportEnv Identifier '=' expression {$node = new Node.ExportEnvNode($ExportEnv, $Identifier, $expression.node);}
	;

forStatement returns [Node node]
	: For '(' forInit ';' forCond ';' forIter ')' block {$node = new Node.ForNode($For, $forInit.node, $forCond.node, $forIter.node, $block.node);}
	;

forInit returns [Node node]
	: variableDeclaration {$node = $variableDeclaration.node;}
	| expression {$node = $expression.node;}
	| assignStatement {$node = $assignStatement.node;}
	| {$node = new Node.EmptyNode();}
	;

forCond returns [Node.ExprNode node]
	: expression {$node = $expression.node;}
	| {$node = new Node.EmptyNode();}
	;

forIter returns [Node node]
	: expression {$node = $expression.node;}
	| assignStatement {$node = $assignStatement.node;}
	| suffixStatement {$node = $suffixStatement.node;}
	| {$node = new Node.EmptyNode();}
	;

foreachStatement returns [Node node]
	: For '(' Identifier 'in' expression ')' block {$node = new Node.ForInNode($For, $Identifier, $expression.node, $block.node);}
	;

ifStatement returns [Node node] locals [ParserUtils.IfElseBlock ifElseBlock]
	: If '(' expression ')' b+=block (Else ei+=ifStatement | Else b+=block)?
		{
			$ifElseBlock = new ParserUtils.IfElseBlock($b.get(0).node);
			if($b.size() > 1) {
				$ifElseBlock.setElseBlockNode($b.get(1).node);
			}
			if($ei.size() > 0) {
				$ifElseBlock.setElseBlockNode($ei.get(0).node);
			}
			$node = new Node.IfNode($If, $expression.node, $ifElseBlock);
		}
	;

importEnvStatement returns [Node node]
	: ImportEnv Identifier {$node = new Node.ImportEnvNode($Identifier);}
	;

importCommandStatement returns [Node node]	//FIXME:
	: {enterCmd();} ImportCmd a+=CommandSymbol+ {exitCmd();}
		{
			$node = new Node.EmptyNode();
			for(int i = 0; i < $a.size(); i++) {
				this.getScope().setCommandPath($a.get(i).getText());
			}
		}
	;

importCommandEnd
	: EOF
	| ';'
	| LineEndInCmd
	;

returnStatement returns [Node node] locals [ParserUtils.ReturnExpr returnExpr]
	: Return e+=expression?
		{
			$returnExpr = new ParserUtils.ReturnExpr();
			if($e.size() == 1) {
				$returnExpr.setNode($e.get(0).node);
			}
			$node = new Node.ReturnNode($Return, $returnExpr.getExprNode());
		}
	;

throwStatement returns [Node node]
	: Throw expression {$node = new Node.ThrowNode($Throw, $expression.node);}
	;

whileStatement returns [Node node]
	: While '(' expression ')' block {$node = new Node.WhileNode($While, $expression.node, $block.node);}
	;

tryCatchStatement returns [Node node] locals [Node.TryNode tryNode]
	: Try block c+=catchStatement+ finallyBlock
		{
			$tryNode = new Node.TryNode($Try, $block.node, $finallyBlock.node);
			for(int i = 0; i < $c.size(); i++) {
				$tryNode.setCatchNode($c.get(i).node);
			}
			$node = $tryNode;
		}
	;

finallyBlock returns [Node node]
	: Finally block {$node = $block.node;}
	| {$node = Node.EmptyBlockNode.INSTANCE;}
	;

catchStatement returns [Node.CatchNode node]
	: Catch '(' exceptDeclaration ')' block
		{
			$node = new Node.CatchNode($Catch, $exceptDeclaration.except.getName(), $exceptDeclaration.except.getTypeSymbol(), $block.node);
		}
	;

exceptDeclaration returns [ParserUtils.CatchedException except]
	: Identifier (':' t+=typeName)?
		{
			$except = new ParserUtils.CatchedException($Identifier);
			if($t.size() == 1) {
				$except.setTypeSymbol($t.get(0).type);
			}
		}
	;

variableDeclaration returns [Node node]
	: flag=(Let | Var) Identifier '=' expression
		{
			$node = new Node.VarDeclNode($flag, $Identifier, $expression.node);
		}
	;

assignStatement returns [Node node]
	: left=expression op=(ASSIGN | ADD_ASSIGN | SUB_ASSIGN | MUL_ASSIGN | DIV_ASSIGN | MOD_ASSIGN) right=expression
		{
			$node = new Node.AssignNode($op, $left.node, $right.node);
		}
	;

emptyStatement returns [Node node]
	: ';' {$node = new Node.EmptyNode();}
	;

suffixStatement returns [Node node]
	: expression op=(INC | DEC) {$node = new Node.AssignNode($expression.node, $op);}
	;

// expression definition.
// command expression
commandExpression returns [Node.ExprNode node]
	: singleCommandExpr {$node = $singleCommandExpr.node;}
	;

singleCommandExpr returns [Node.CommandNode node]
	: CommandName a+=commandArg*
		{
			$node = new Node.CommandNode($CommandName, getScope().resolveCommandPath($CommandName.getText()));
			for(int i = 0; i < $a.size(); i++) {
				$node.setArg($a.get(i).node);
			}
		}
	;

commandArg returns [Node.ExprNode node]
	: CommandArg {$node = new Node.StringValueNode($CommandArg);}
	;

// normal expression
expression returns [Node.ExprNode node]
	: primaryExpression {$node = $primaryExpression.node;}
	| a=expression arguments {$node = new Node.ApplyNode($a.node, $arguments.args);}
	| r=expression LeftBracket i=expression RightBracket {$node = new Node.ElementGetterNode($LeftBracket, $r.node, $i.node);}
	| a=expression '.' Identifier {$node = new Node.FieldGetterNode($a.node, $Identifier);}
	| New typeName arguments {$node = new Node.ConstructorCallNode($New, $typeName.type, $arguments.args);}
	| '(' typeName ')' right=expression {$node = new Node.CastNode($typeName.type, $right.node);}
	| op=(ADD | SUB | BIT_NOT | NOT) right=expression {$node = new Node.OperatorCallNode($op, $right.node);}
	| left=expression op=(MUL | DIV | MOD) right=expression {$node = new Node.OperatorCallNode($op, $left.node, $right.node);}
	| left=expression op=(ADD | SUB) right=expression {$node = new Node.OperatorCallNode($op, $left.node, $right.node);}
	| left=expression op=(LT | LE | GT | GE) right=expression {$node = new Node.OperatorCallNode($op, $left.node, $right.node);}
	| left=expression Instanceof typeName {$node = new Node.InstanceofNode($Instanceof, $left.node, $typeName.type);}
	| left=expression op=(EQ | NE | REGEX_MATCH | REGEX_UNMATCH) right=expression {$node = new Node.OperatorCallNode($op, $left.node, $right.node);}
	| left=expression AND right=expression {$node = new Node.OperatorCallNode($AND, $left.node, $right.node);}
	| left=expression XOR right=expression {$node = new Node.OperatorCallNode($XOR, $left.node, $right.node);}
	| left=expression OR right=expression {$node = new Node.OperatorCallNode($OR, $left.node, $right.node);}
	| left=expression COND_AND right=expression {$node = new Node.CondOpNode($COND_AND, $left.node, $right.node);}
	| left=expression COND_OR right=expression {$node = new Node.CondOpNode($COND_OR, $left.node, $right.node);}
	;

primaryExpression returns [Node.ExprNode node]
	: literal {$node = $literal.node;}
	| symbol {$node = $symbol.node;}
	| '(' expression ')' {$node = $expression.node;}
	;

symbol returns [Node.ExprNode node]
	: Identifier {$node = new Node.SymbolNode($Identifier);}
	;

literal returns [Node.ExprNode node]
	: IntLiteral {$node = new Node.IntValueNode($IntLiteral);}
	| FloatLiteral {$node = new Node.FloatValueNode($FloatLiteral);}
	| BooleanLiteral {$node = new Node.BooleanValueNode($BooleanLiteral);}
	| StringLiteral {$node = new Node.StringValueNode($StringLiteral);}
	| arrayLiteral {$node = $arrayLiteral.node;}
	| mapLiteral {$node = $mapLiteral.node;}
	| pairLiteral {$node = $pairLiteral.node;}
	;

arrayLiteral returns [Node.ExprNode node] locals [Node.ArrayNode arrayNode]
	: LeftBracket expr+=expression (',' expr+=expression)* RightBracket
		{	$arrayNode = new Node.ArrayNode($LeftBracket);
			for(int i = 0; i < $expr.size(); i++) {
				$arrayNode.addNode($expr.get(i).node);
			}
			$node = $arrayNode;
		}
	;

mapLiteral returns [Node.ExprNode node] locals [Node.MapNode mapNode]
	: LeftBrace entrys+=mapEntry (',' entrys+=mapEntry)* RightBrace
		{
			$mapNode = new Node.MapNode($LeftBrace);
			for(int i = 0; i < $entrys.size(); i++) {
				$mapNode.addEntry($entrys.get(i).entry.keyNode, $entrys.get(i).entry.valueNode);
			}
			$node = $mapNode;
		}
	;

mapEntry returns [ParserUtils.MapEntry entry]
	: key=expression ':' value=expression {$entry = new ParserUtils.MapEntry($key.node, $value.node);}
	;

pairLiteral returns [Node.ExprNode node]
	: LeftParenthese left=expression ',' right=expression RightParenthese
		{
			$node = new Node.PairNode($LeftParenthese, $left.node, $right.node);
		}
	;

arguments returns [ParserUtils.Arguments args]
	: '(' a+=argumentList? ')'
		{
			$args = new ParserUtils.Arguments();
			if($a.size() == 1) {
				$args = $a.get(0).args;
			}
		}
	;

argumentList returns [ParserUtils.Arguments args]
	: a+= expression (',' a+=expression)* 
		{
			$args = new ParserUtils.Arguments();
			for(int i = 0; i < $a.size(); i++) {
				$args.addNode($a.get(i).node);
			}
		}
	;

