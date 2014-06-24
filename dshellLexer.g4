lexer grammar dshellLexer;

@header {
package dshell.internal.parser;
import dshell.internal.parser.Node;
import dshell.internal.parser.ParserUtils;
import dshell.internal.parser.TypeSymbol;
import dshell.internal.parser.error.ParserErrorHandler;
}

@members {
private final CommandScope cmdScope = new CommandScope();

private boolean trace = false;

@Override
public Token nextToken() {
	Token token = super.nextToken();
	if(this.trace) {
		System.err.println("@nextToken: " + token);
	}
	return token;
}

@Override
public void recover(LexerNoViableAltException e) {
	ParserErrorHandler.reportError(e);
}

@Override
public void recover(RecognitionException e) {
	ParserErrorHandler.reportError(e);
}

private boolean requireCommand = false;
private boolean requireCommand() {
	return this.requireCommand;
}

public void enterCmd() {
	if(this.trace) System.err.println("enter command");
	this.requireCommand = true;
}

public void exitCmd() {
	if(this.trace) System.err.println("exit command");
	this.requireCommand = false;
}

public CommandScope getScope() {
	return this.cmdScope;
}

public void setTrace(boolean trace) {
	this.trace = trace;
}
}

// ######################
// #        lexer       #
// ######################

// reserved keyword
Assert      : 'assert';
Break       : 'break';
Boolean     : 'boolean';
Catch       : 'catch';
Continue    : 'continue';
Class       : 'class';
Constructor : 'constructor';
Do          : 'do';
Else        : 'else';
Extends     : 'extends';
ExportEnv   : 'export-env';
Func        : 'Func';
Function    : 'function';
Finally     : 'finally';
Float       : 'float';
For         : 'for';
If          : 'if';
ImportCmd   : 'import-command';
ImportEnv   : 'import-env';
In          : 'in';
Int         : 'int';
Instanceof  : 'instanceof';
Let         : 'let';
New         : 'new';
Return      : 'return';
Super       : 'super';
Try         : 'try';
Throw       : 'throw';
Var         : 'var';
Void        : 'void';
While       : 'while';

LeftParenthese  : '(';
RightParenthese : ')';
LeftBracket     : '[';
RightBracket    : ']';
LeftBrace       : '{';
RightBrace      : '}';

Colon           : ':';
Semicolon       : ';';
Comma           : ',';
Period          : '.';

// operator
// binary op
ADD           : '+';
SUB           : '-';
MUL           : '*';
DIV           : '/';
MOD           : '%';
LT            : '<';
GT            : '>';
LE            : '<=';
GE            : '>=';
EQ            : '==';
NE            : '!=';
AND           : '&';
OR            : '|';
XOR           : '^';
COND_AND      : '&&';
COND_OR       : '||';
REGEX_MATCH   : '=~';
REGEX_UNMATCH : '!~';

// prefix op
BIT_NOT       : '~';
NOT           : '!';

// suffix op
INC           : '++';
DEC           : '--';

// assign op
ASSIGN        : '=';
ADD_ASSIGN    : '+=';
SUB_ASSIGN    : '-=';
MUL_ASSIGN    : '*=';
DIV_ASSIGN    : '/=';
MOD_ASSIGN    : '%=';


// literal
// int literal	//TODO: hex, oct number
fragment
Number
	: '0'
	| [1-9] [0-9]*
	;
IntLiteral
	: Number
	;

// float literal	//TODO: exp
FloatLiteral
	: Number '.' Number FloatSuffix?
	;

fragment
FloatSuffix
	: [eE] [+-]? Number
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
CommandName	//FIXME:
	: ~[\n\t\r\u0020|#&$'"\\;<>()]+ {cmdScope.isCommand(getText())}? -> mode(CMD_ARG)
	;

CommandSymbol
	: ~[\n\t\r\u0020|#&$'"\\;<>()]+ {requireCommand()}?
	;

Identifier
	: [_a-zA-Z] [_0-9a-zA-Z]*
	;


// comment & space
Comment
	: '#' ~[\r\n\u2028\u2029]* -> skip
	;
WhiteSpace
	: [\t\u000B\u000C\u0020\u00A0]+ -> skip
	;

LineEndInCmd
	: {requireCommand()}? [\r\n\u2028\u2029]
	;
LineEnd
	: [\r\n\u2028\u2029] -> channel(HIDDEN)
	;

// command arg mode
mode CMD_ARG;
CommandArg
	: ~[\n\t\r\u0020|#&$'"\\;<>()]+
	;

InnerComment
	: '#' ~[\r\n\u2028\u2029]* -> skip
	;
InnerWhiteSpace
	: [\t\u000B\u000C\u0020\u00A0]+ -> skip
	;

CommandEnd
	: ([\n\r&;] | '||' | '&&' ) -> mode(DEFAULT_MODE)
	;

