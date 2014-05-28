grammar dshell;

@header {
package dshell.internal.parser;
import dshell.internal.parser.TypePool;
import dshell.internal.parser.Node;
import dshell.internal.parser.NodeUtils;
}

// ######################
// #        parse       #
// ######################

toplevel returns [Node.RootNode node]
	: a+=toplevelStatements? EOF
		{
			if($a.size() == 0) {
				$node = new Node.RootNode();
			} else {
				$node = $a.get(0).node;
			}
		}
	;
toplevelStatements returns [Node.RootNode node]
	: a+=toplevelStatement (StmtEnd a+=toplevelStatement)*
	 {
	 	$node = new Node.RootNode();
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
functionDeclaration returns [Node node]
	: Function SymbolName '(' argumentsDeclaration ')' block
		{$node = new Node.FunctionNode($Function, $SymbolName, $argumentsDeclaration.decl, $block.node);}
	;
argumentsDeclaration returns [NodeUtils.ArgsDecl decl]
	: a+=variableDeclarationWithType (',' a+=variableDeclarationWithType)*
		{
			$decl = new NodeUtils.ArgsDecl();
			for(int i = 0; i < $a.size(); i++) {
				$decl.addArgDecl($a.get(0).arg);
			}
		}
	| { $decl = new NodeUtils.ArgsDecl();}
	;
variableDeclarationWithType returns [NodeUtils.ArgDecl arg]
	: SymbolName ':' typeName {$arg = new NodeUtils.ArgDecl($SymbolName, $typeName.type);}
	;
typeName returns [TypePool.Type type] locals [TypePool.Type[] types]
	: Int {$type = TypePool.getInstance().intType;}
	| Float {$type = TypePool.getInstance().floatType;}
	| Boolean {$type = TypePool.getInstance().booleanType;}
	| Void {$type = TypePool.getInstance().voidType;}
	| ClassName {$type = TypePool.getInstance().getClassType(NodeUtils.resolveClassName($ClassName));}
	| typeName '[]' {$type = TypePool.getInstance().createAndGetArrayTypeIfUndefined($typeName.type);}
	| 'Map' '<' typeName '>' {$type = TypePool.getInstance().createAndGetMapTypeIfUndefined($typeName.type);}
	| 'Func' '<' typeName ',' paramTypes '>'
		{$type = TypePool.getInstance().createAndGetFuncTypeIfUndefined($typeName.type, $paramTypes.types);}
	| ClassName '<' a+=typeName (',' a+=typeName)* '>'
		{
			$types = new TypePool.Type[$a.size()];
			for(int i = 0; i < $types.length; i++) {
				$types[i] = $a.get(i).type;
			}
			$type = TypePool.getInstance().createAndGetGenericTypeIfUndefined(NodeUtils.resolveClassName($ClassName), $types);
		}
	;
paramTypes returns [TypePool.Type[] types]
	: '[' a+=typeName (',' a+=typeName)* ']'
		{
			$types = new TypePool.Type[$a.size()];
			for(int i = 0; i < $types.length; i++) {
				$types[i] = $a.get(i).type;
			}
		}
	;
block returns [Node node] locals [NodeUtils.Block blockModel]
	: '{' b+=blockStatement* '}' 
		{
			for(int i = 0; i < $b.size(); i++) {
				$blockModel.addNode($b.get(i).node);
			}
			$node = new Node.BlockNode($blockModel);
		}
	;
blockStatement returns [Node node]
	: statement StmtEnd {$node = $statement.node;}
	;
classDeclaration returns [Node node] locals [NodeUtils.SuperTypeResolver resolver]
	: Class ClassName (Extends a+=typeName)? classBody
		{
			$resolver = new NodeUtils.SuperTypeResolver();
			if($a.size() == 1) {
				$resolver.setType($a.get(0).type);
			}
			$node = new Node.ClassNode($Class, NodeUtils.resolveClassName($ClassName), $resolver.getType(), $classBody.body);
		}
	;
classBody returns [NodeUtils.ClassBody body]
	: '{' (a+=classElement StmtEnd)+ '}'
		{
			$body = new NodeUtils.ClassBody();
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
	: assertStatement {$node = $assertStatement.node;}
	| breakStatement {$node = $breakStatement.node;}
	| continueStatement {$node = $continueStatement.node;}
	| exportEnvStatement {$node = $exportEnvStatement.node;}
	| forStatement {$node = $forStatement.node;}
	| foreachStatement {$node = $foreachStatement.node;}
	| ifStatement {$node = $ifStatement.node;}
	| importEnvStatement {$node = $importEnvStatement.node;}
	| importCommandStatement {$node = $importCommandStatement.node;}
	| returnStatement {$node = $returnStatement.node;}
	| throwStatement {$node = $throwStatement.node;}
	| whileStatement {$node = $whileStatement.node;}
	| tryCatchStatement {$node = $tryCatchStatement.node;}
	| variableDeclaration {$node = $variableDeclaration.node;}
	| assignStatement {$node = $assignStatement.node;}
	| expression {$node = $expression.node;}
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
	: Export 'env' SymbolName '=' expression {$node = new Node.ExportEnvNode($Export, $expression.node);}
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
	: For '(' SymbolName 'in' expression ')' block {$node = new Node.ForInNode($For, $SymbolName, $expression.node, $block.node);}
	;
ifStatement returns [Node node] locals [NodeUtils.IfElseBlock ifElseBlock]
	: If '(' expression ')' b+=block (Else b+=block)?
		{
			$ifElseBlock = new NodeUtils.IfElseBlock($b.get(0).node);
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
returnStatement returns [Node node] locals [NodeUtils.ReturnExpr returnExpr]
	: Return e+=expression?
		{
			$returnExpr = new NodeUtils.ReturnExpr();
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
	: Catch '(' exceptDeclaration ')' block { $node = new Node.CatchNode($Catch, $exceptDeclaration.except, $block.node);}
	;
exceptDeclaration returns [NodeUtils.CatchedException except]
	: SymbolName (':' t+=typeName)?
		{
			$except = new NodeUtils.CatchedException($SymbolName);
			if($t.size() == 1) {
				$except.setType($t.get(0).type);
			}
		}
	;
variableDeclaration returns [Node node] locals [Node.VarDeclNode varDeclNode]
	: flag=(Let | Var) SymbolName (':' t+=typeName)? '=' expression
		{
			$varDeclNode = new Node.VarDeclNode($flag, $SymbolName, $expression.node);
			if($t.size() == 1) {
				$varDeclNode.setValueType($t.get(0).type);
			}
			$node = $varDeclNode;
		}
	;
assignStatement returns [Node node]
	: left=expression op=(ASSIGN | ADD_ASSIGN | SUB_ASSIGN | MUL_ASSIGN | DIV_ASSIGN | MOD_ASSIGN) right=expression
		{
			$node = new Node.AssignNode($op, $left.node, $right.node);
		}
	;
expression returns [Node node] //FIXME: right join
	: literal {$node = $literal.node;}
	| symbol {$node = $symbol.node;}
	| '(' expression ')' {$node = $expression.node;}
	| expression '.' SymbolName {$node = new Node.FieldGetterNode($expression.node, $SymbolName);}
	| New ClassName '(' arguments ')' {$node = new Node.ConstructorCallNode($New, NodeUtils.resolveClassName($ClassName), $arguments.args);}
	| expression '.' SymbolName '(' arguments ')' {$node = new Node.MethodCallNode($expression.node, $SymbolName, $arguments.args);}
	| SymbolName '(' arguments ')' {$node = new Node.FuncCallNode($SymbolName, $arguments.args);}
	| r=expression '[' i=expression ']' {$node = new Node.ElementGetterNode($r.node, $i.node);}
	| '(' typeName ')' expression {$node = new Node.CastNode($typeName.type, $expression.node);}
	| expression op=(INC | DEC) {$node = new Node.SuffixIncrementNode($expression.node, $op);}
	| op=(PLUS | MINUS) expression {$node = new Node.FuncCallNode($op, $expression.node);}
	| op=(BIT_NOT | NOT) expression {$node = new Node.FuncCallNode($op, $expression.node);}
	| left=expression op=(MUL | DIV | MOD) right=expression {$node = new Node.FuncCallNode($op, $left.node, $right.node);}
	| left=expression op=(ADD | SUB) right=expression {$node = new Node.FuncCallNode($op, $left.node, $right.node);}
	| left=expression op=(LT | LE | GT | GE) right=expression {$node = new Node.FuncCallNode($op, $left.node, $right.node);}
	| expression Instanceof typeName {$node = new Node.InstanceofNode($Instanceof, $expression.node, $typeName.type);}
	| left=expression op=(EQ | NE) right=expression {$node = new Node.FuncCallNode($op, $left.node, $right.node);}
	| left=expression op=(AND | OR | XOR) right=expression {$node = new Node.FuncCallNode($op, $left.node, $right.node);}
	| left=expression op=(COND_AND | COND_OR) right=expression {$node = new Node.CondOpNode($op, $left.node, $right.node);}
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
				$mapNode.addEntry($entrys.get(i).entry);
			}
			$node = $mapNode;
		}
	;
mapEntry returns [NodeUtils.MapEntry entry]
	: key=expression ':' value=expression {$entry = new NodeUtils.MapEntry($key.node, $value.node);}
	;
arguments returns [NodeUtils.Arguments args]
	: a+=argument (',' a+=argument)* 
		{
			$args = new NodeUtils.Arguments();
			for(int i = 0; i < $a.size(); i++) {
				$args.addNode($a.get(i).node);
			}
		}
	| {$args = new NodeUtils.Arguments();}
	;
argument returns [Node node]
	: expression {$node = $expression.node;}
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
Null		: 'null';
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
	: [A-Z] [0-9a-zA-Z]*
	;
CommandName	//FIXME:
	: [0-9a-zA-Z]+
	;

// statement end
StmtEnd
	: StmtEndEach+
	;
fragment
StmtEndEach
	: ';'
	| [\r\n]
	;

// comment & space
Comment
	: '#' ~[\r\n]* -> skip
	;
WhileSpace
	: [ \t\r\n\u000C]+ -> skip
	;
