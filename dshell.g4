grammar dshell;

@header {
package dshell.internal.parser;
import dshell.internal.parser.Node;
import dshell.internal.parser.ParserUtils;
import dshell.internal.parser.TypeSymbol;
}

@parser::members {
	private boolean isLineEnd() {
		int lineEndIndex = this.getCurrentToken().getTokenIndex() - 1;
		Token lineEndToken = _input.get(lineEndIndex);
		if(lineEndToken.getChannel() != Lexer.HIDDEN) {
			return false;
		}
		int type = lineEndToken.getType();
		return type == LineEnd;
	}
}

// ######################
// #        parse       #
// ######################

toplevel returns [Node.RootNode node]
	: (a+=toplevelStatement)+ EOF
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
	| ';'
	| {isLineEnd()}?
	;
functionDeclaration returns [Node node]
	: Function SymbolName '(' argumentsDeclaration ')' returnType block
		{$node = new Node.FunctionNode($Function, $SymbolName, $returnType.type, $argumentsDeclaration.decl, $block.node);}
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
	: SymbolName ':' typeName {$arg = new ParserUtils.ArgDecl($SymbolName, $typeName.type);}
	;
typeName returns [TypeSymbol type] locals [TypeSymbol[] types]
	: Int {$type = TypeSymbol.toPrimitive($Int);}
	| Float {$type = TypeSymbol.toPrimitive($Float);}
	| Boolean {$type = TypeSymbol.toPrimitive($Boolean);}
	| Void {$type = TypeSymbol.toVoid($Void);}
	| SymbolName {$type = TypeSymbol.toClass($SymbolName);}
	| typeName '[]' {$type = TypeSymbol.toArray($typeName.type);}
	| 'Map' '<' typeName '>' {$type = TypeSymbol.toMap($typeName.type);}
	| 'Func' '<' typeName paramTypes '>'
		{$type = TypeSymbol.toFunc($typeName.type, $paramTypes.types);}
	| ClassName '<' a+=typeName (',' a+=typeName)* '>'
		{
			$types = new TypeSymbol[$a.size()];
			for(int i = 0; i < $types.length; i++) {
				$types[i] = $a.get(i).type;
			}
			$type = TypeSymbol.toGeneric($ClassName, $types);
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
	: '{' b+=statement+ '}' 
		{
			$blockModel = new ParserUtils.Block();
			for(int i = 0; i < $b.size(); i++) {
				$blockModel.addNode($b.get(i).node);
			}
			$node = new Node.BlockNode($blockModel);
		}
	;
classDeclaration returns [Node node] locals [String superName]
	: Class name=ClassName (Extends a+=ClassName)? classBody
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
	| breakStatement statementEnd {$node = $breakStatement.node;}
	| continueStatement statementEnd {$node = $continueStatement.node;}
	| exportEnvStatement statementEnd {$node = $exportEnvStatement.node;}
	| forStatement {$node = $forStatement.node;}
	| foreachStatement {$node = $foreachStatement.node;}
	| ifStatement {$node = $ifStatement.node;}
	| importEnvStatement statementEnd {$node = $importEnvStatement.node;}
	| importCommandStatement statementEnd {$node = $importCommandStatement.node;}
	| returnStatement statementEnd {$node = $returnStatement.node;}
	| throwStatement statementEnd {$node = $throwStatement.node;}
	| whileStatement {$node = $whileStatement.node;}
	| tryCatchStatement {$node = $tryCatchStatement.node;}
	| variableDeclaration statementEnd {$node = $variableDeclaration.node;}
	| assignStatement statementEnd {$node = $assignStatement.node;}
	| expression statementEnd {$node = $expression.node;}
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
exportEnvStatement returns [Node node]	//FIXME:
	: Export 'env' SymbolName '=' expression {$node = new Node.ExportEnvNode($Export, $SymbolName, $expression.node);}
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
forCond returns [Node node]
	: expression {$node = $expression.node;}
	| {$node = new Node.EmptyNode();}
	;
forIter returns [Node node]
	: expression {$node = $expression.node;}
	| assignStatement {$node = $assignStatement.node;}
	| {$node = new Node.EmptyNode();}
	;
foreachStatement returns [Node node]
	: For '(' SymbolName In expression ')' block {$node = new Node.ForInNode($For, $SymbolName, $expression.node, $block.node);}
	;
ifStatement returns [Node node] locals [ParserUtils.IfElseBlock ifElseBlock]
	: If '(' expression ')' b+=block (Else b+=block)?
		{
			$ifElseBlock = new ParserUtils.IfElseBlock($b.get(0).node);
			if($b.size() > 1) {
				$ifElseBlock.setElseBlockNode($b.get(1).node);
			}
			$node = new Node.IfNode($If, $expression.node, $ifElseBlock);
		}
	;
importEnvStatement returns [Node node]	//FIXME:
	: Import 'env' SymbolName {$node = new Node.ImportEnvNode($SymbolName);}
	;
importCommandStatement returns [Node node]	//FIXME:
	: Import Command {$node = new Node.EmptyNode();}
	;
returnStatement returns [Node node] locals [ParserUtils.ReturnExpr returnExpr]
	: Return e+=expression?
		{
			$returnExpr = new ParserUtils.ReturnExpr();
			if($e.size() == 1) {
				$returnExpr.setNode($e.get(0).node);
			}
			$node = new Node.ReturnNode($Return, $returnExpr);
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
	| {$node = new Node.EmptyBlockNode();}
	;
catchStatement returns [Node.CatchNode node]
	: Catch '(' exceptDeclaration ')' block
		{
			$node = new Node.CatchNode($Catch, $exceptDeclaration.except.getName(), $exceptDeclaration.except.getTypeSymbol(), $block.node);
		}
	;
exceptDeclaration returns [ParserUtils.CatchedException except]
	: SymbolName (':' t+=typeName)?
		{
			$except = new ParserUtils.CatchedException($SymbolName);
			if($t.size() == 1) {
				$except.setTypeSymbol($t.get(0).type);
			}
		}
	;
variableDeclaration returns [Node node]
	: flag=(Let | Var) SymbolName '=' expression
		{
			$node = new Node.VarDeclNode($flag, $SymbolName, $expression.node);
		}
	;
assignStatement returns [Node node]
	: left=expression op=(ASSIGN | ADD_ASSIGN | SUB_ASSIGN | MUL_ASSIGN | DIV_ASSIGN | MOD_ASSIGN) right=expression
		{
			$node = new Node.AssignNode($op, $left.node, $right.node);
		}
	;
expression returns [Node node] //FIXME: right join
	: a=expression '.' SymbolName {$node = new Node.FieldGetterNode($a.node, $SymbolName);}
	| New classType arguments {$node = new Node.ConstructorCallNode($New, $classType.type, $arguments.args);}
	| a=expression '.' SymbolName arguments {$node = new Node.MethodCallNode($a.node, $SymbolName, $arguments.args);}
	| SymbolName arguments {$node = new Node.FuncCallNode($SymbolName, $arguments.args);}
	| r=expression '[' i=expression ']' {$node = new Node.ElementGetterNode($r.node, $i.node);}
	| '(' typeName ')' a=expression {$node = new Node.CastNode($typeName.type, $a.node);}
	| symbol op=(INC | DEC) {$node = new Node.SuffixIncrementNode($symbol.node, $op);}
	| op=(PLUS | MINUS) a=expression {$node = new Node.OperatorCallNode($op, $a.node);}
	| op=(BIT_NOT | NOT) a=expression {$node = new Node.OperatorCallNode($op, $a.node);}
	| left=expression op=(MUL | DIV | MOD) right=expression {$node = new Node.OperatorCallNode($op, $left.node, $right.node);}
	| left=expression op=(ADD | SUB) right=expression {$node = new Node.OperatorCallNode($op, $left.node, $right.node);}
	| left=expression op=(LT | LE | GT | GE) right=expression {$node = new Node.OperatorCallNode($op, $left.node, $right.node);}
	| left=expression Instanceof typeName {$node = new Node.InstanceofNode($Instanceof, $left.node, $typeName.type);}
	| left=expression op=(EQ | NE) right=expression {$node = new Node.OperatorCallNode($op, $left.node, $right.node);}
	| left=expression op=(AND | OR | XOR) right=expression {$node = new Node.OperatorCallNode($op, $left.node, $right.node);}
	| left=expression op=(COND_AND | COND_OR) right=expression {$node = new Node.CondOpNode($op, $left.node, $right.node);}
	| primary {$node = $primary.node;}
	;
classType returns [TypeSymbol type]
	: ClassName {$type = TypeSymbol.toClass($ClassName);}
	;
primary returns [Node node]
	: literal {$node = $literal.node;}
	| symbol {$node = $symbol.node;}
	| '(' expression ')' {$node = $expression.node;}
	;
symbol returns [Node node]
	: SymbolName {$node = new Node.SymbolNode($SymbolName);}
	;
literal returns [Node node]
	: IntLiteral {$node = new Node.IntValueNode($IntLiteral);}
	| FloatLiteral {$node = new Node.FloatValueNode($FloatLiteral);}
	| BooleanLiteral {$node = new Node.BooleanValueNode($BooleanLiteral);}
	| StringLiteral {$node = new Node.StringValueNode($StringLiteral);}
	| NullLiteral {$node = new Node.NullNode($NullLiteral);}
	| arrayLiteral {$node = $arrayLiteral.node;}
	| mapLiteral {$node = $mapLiteral.node;}
	;
arrayLiteral returns [Node node] locals [Node.ArrayNode arrayNode]
	: '[' expr+=expression (',' expr+=expression)* ']' 
		{	$arrayNode = new Node.ArrayNode();
			for(int i = 0; i < $expr.size(); i++) {
				$arrayNode.addNode($expr.get(i).node);
			}
			$node = $arrayNode;
		}
	;
mapLiteral returns [Node node] locals [Node.MapNode mapNode]
	: '{' entrys+=mapEntry (',' entrys+=mapEntry)* '}'
		{
			$mapNode = new Node.MapNode();
			for(int i = 0; i < $entrys.size(); i++) {
				$mapNode.addEntry($entrys.get(i).entry.keyNode, $entrys.get(i).entry.valueNode);
			}
			$node = $mapNode;
		}
	;
mapEntry returns [ParserUtils.MapEntry entry]
	: key=expression ':' value=expression {$entry = new ParserUtils.MapEntry($key.node, $value.node);}
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

// ######################
// #        lexer       #
// ######################

// reserved keyword
Assert		: 'assert';
Break		: 'break';
Boolean		: 'boolean';
Catch		: 'catch';
Continue	: 'continue';
Class		: 'class';
Command		: 'command';
Constructor	: 'constructor';
Do			: 'do';
Else		: 'else';
Extends		: 'extends';
Export		: 'export';
Function	: 'function';
Finally		: 'finally';
Float		: 'float';
For			: 'for';
If			: 'if';
Import		: 'import';
In			: 'in';
Int			: 'int';
Instanceof	: 'instanceof';
Let			: 'let';
New			: 'new';
Return		: 'return';
Super		: 'super';
Try			: 'try';
Throw		: 'throw';
Var			: 'var';
Void		: 'void';
While		: 'while';

// operator
// binary op
ADD		: '+';
SUB		: '-';
MUL		: '*';
DIV		: '/';
MOD		: '%';
LT		: '<';
GT		: '>';
LE		: '<=';
GE		: '>=';
EQ		: '==';
NE		: '!=';
AND		: '&';
OR		: '|';
XOR		: '^';
COND_AND	: '&&';
COND_OR		: '||';

// prefix op
PLUS	: '+';
MINUS	: '-';
BIT_NOT	: '~';
NOT		: '!';

// suffix op
INC		: '++';
DEC		: '--';

// assign op
ASSIGN	: '=';
ADD_ASSIGN	: '+=';
SUB_ASSIGN	: '-=';
MUL_ASSIGN	: '*=';
DIV_ASSIGN	: '/=';
MOD_ASSIGN	: '%=';


// literal
// int literal	//TODO: hex, oct number
fragment
Number
	: '0'
	| [1-9] [0-9]*
	;
IntLiteral
	: [+-]? Number
	;

// float literal	//TODO: exp
FloatLiteral
	: [+-]? Number '.' Number
	;

// boolean literal
BooleanLiteral
	: 'true'
	| 'false'
	;

// String literal	//TODO: interpolation
StringLiteral
	: '"' StringChars? '"'
	| '\'' StringChars? '\''
	;
fragment
StringChars
	: StringChar+
	;
fragment
StringChar
	: ~["\\]
	| EscapeSequence
	;
fragment
EscapeSequence	// TODO: unicode escape
	: '\\' [btnfr"'\\]
	;

// null literal
NullLiteral
	: 'null'
	;

// symbol , class and command name
SymbolName
	: [_a-zA-Z] [_0-9a-zA-Z]*
	;
ClassName
	: [A-Z] [_0-9a-zA-Z]*
	;
CommandName	//FIXME:
	: [0-9a-zA-Z]+
	;

// comment & space
Comment
	: '#' ~[\r\n]* -> skip
	;
//WhiteSpace
//	: [ \t\r\n\u000C]+ -> skip
//	;
WhiteSpace
	: [ \t\u000C]+ -> channel(HIDDEN)
	;
LineEnd
	: [\r\n] -> channel(HIDDEN)
	;
