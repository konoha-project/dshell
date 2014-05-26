grammar dshell;

// ######################
// #        parse       #
// ######################

toplevel
	: toplevelStatements? EOF
	;
toplevelStatements
	: toplevelStatement (StmtEnd toplevelStatement)*
	;
toplevelStatement
	: functionDeclaration
	| classDeclaration
	| statement
	;
functionDeclaration
	: Function SymbolName '(' argumentsDeclaration? ')' block
	;
argumentsDeclaration
	: variableDeclarationWithType (',' variableDeclarationWithType)*
	;
variableDeclarationWithType
	: SymbolName ':' typeName
	;
typeName
	: Int
	| Float
	| Boolean
	| Void
	| ClassName
	| typeName '[]'
	| 'Map' '<' typeName '>'
	| 'Func' '<' typeName (',' typeName)* '>'
	;
block
	: '{' blockStatement* '}'
	;
blockStatement
	: statement StmtEnd
	;
classDeclaration
	: Class ClassName (Extends ClassName)? classBlock
	;
classBlock
	: '{' (classElement StmtEnd)+ '}'
	;
classElement
	: fieldDeclaration
	| functionDeclaration
	| constructorDeclaration
	;
fieldDeclaration
	: variableDeclaration
	;
constructorDeclaration
	: Constructor '(' arguments? ')' block
	;
statement
	: Assert roundExpression
	| Break expression
	| Continue expression
	| Export 'env' SymbolName '=' expression	//FIXME:
	| For forCond block
	| For forInCond block
	| If roundExpression block (Else block)?
	| Import 'env' SymbolName	//FIXME:
	| Import Command			// FIXME:
	| Return expression
	| Throw expression
	| While roundExpression block
	| tryCatchStatement
	| variableDeclaration
	| assignStatement
	| expression
	;
tryCatchStatement
	: Try block (Catch '(' variableDeclarationWithType ')' block)+ (Finally block)?
	;
forCond
	: '(' (variableDeclaration | expression | assignStatement)? ';' expression? ';' (expression | assignStatement)? ')'
	;
forInCond
	: '(' SymbolName In expression ')'
	;
variableDeclaration
	: (Let | Var) SymbolName (':' typeName)? '=' expression
	;
assignStatement
	: expression ('=' | '+=' | '-=' | '*=' | '/=' | '%=') expression
	;
roundExpression
	: '(' expression ')'
	;
expression
	: primary
	| expression '.' SymbolName
	| expression '.' This
	| New ClassName '(' arguments? ')'
	| expression '.' SymbolName '(' arguments? ')'
	| SymbolName '(' arguments? ')'
	| expression '[' expression ']'
	| '(' typeName ')' expression
	| expression ('++' | '--')
	| ('+' | '-') expression
	| ('~' | '!') expression
	| expression ('*' | '/' | '%') expression
	| expression ('+' | '-') expression
	| expression ('<' | '<=' | '>' | '>=') expression
	| expression Instanceof typeName
	| expression ('==' | '!=') expression
	| expression ('&' | '|' | '^') expression
	| expression ('&&' | '||') expression
	;
primary
	: literal
	| This
	| SymbolName
	| '(' expression ')'
	;
literal
	: IntLiteral
	| FloatLiteral
	| BooleanLiteral
	| StringLiteral
	| NullLiteral
	| arrayLiteral
	| mapLiteral
	;
arrayLiteral
	: '[' expression (',' expression)* ']'
	;
mapLiteral
	: '{' StringLiteral ':' expression (',' StringLiteral ':' expression)* '}'
	;
arguments
	: argument (',' argument)*
	;
argument
	: expression
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
This		: 'this';
Throw		: 'throw';
Var			: 'var';
Void		: 'void';
While		: 'while';

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
