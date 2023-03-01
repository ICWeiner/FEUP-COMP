grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;

/*
program
    : (importDeclaration)* classDeclaration EOF
    ;

importDeclaration
    : 'import' ID ('.' ID)* ';'
    ;
classDeclaration
    : 'class' ID ('extends' ID)? '{' (varDeclaration)* (methodDeclaration)* '}'
    ;

varDeclaration
    : type ID ';'
    ;

methodDeclaration
    : ('public')? type ID '(' (type ID (',' type ID)* )? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}'
    | ('public')? 'static' 'void' 'main' '(' 'String' '[' ']' ID ')' '{' (varDeclaration)* (statement)* '}'
    ;

type
    : 'int' '[' ']'
    | 'boolean'
    | 'int'
    | ID
    ;

statement
    : '{' ( statement )* '}' #Curly
    | 'if' '(' expression ')' statement 'else' statement #IFELSE
    | 'while' '(' expression ')' statement #While
    | expression ';' # ExprStmt
    | var=ID '=' expression ';' #Assignment
    | ID '[' expression ']' '=' expression ';' #AAAAAAAA
    ;

expression
    : expression ('&&' | '<' | '+' | '-' | '*' | '/' ) expression #BinaryOp
    | expression '[' expression ']' #ArrayParenthesis
    | expression '.' 'length' #Length
    | expression '.' ID '(' ( expression ( ',' expression )* )? ')' #Function
    | 'new' 'int' '[' expression ']' #ArrayDeclaration
    | 'new' ID '(' ')' #Declaration
    | '!' expression #Unary
    | '(' expression ')' #Parenthesis
    | INT #Integer
    | 'true' #TF
    | 'false' #TF
    | ID #Identifier
    | 'this' #This
    ;
*/

program
    : (importDeclaration)* classDeclaration EOF
    | statement+ EOF
    ;

importDeclaration
    : 'import' ID ('.' ID)* ';'
    ;

classDeclaration
    : 'class' ID ('extends' ID)? '{' (varDeclaration)* (methodDeclaration)* '}'
    ;

varDeclaration
    : type ID ';'
    ;

methodDeclaration
    : ('public')? type ID '(' (type ID (',' type ID)* )? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}'
    | ('public')? 'static' 'void' 'main' '(' ID '[' ']' ID ')' '{' (varDeclaration)* (statement)* '}'
    ;

type
    : 'int' '[' ']'
    | 'boolean'
    | 'int'
    | ID
    ;

statement
    : '{' ( statement )* '}'
    | 'if' '(' expression ')' statement 'else' statement
    | 'while' '(' expression ')' statement
    | expression ';'
    | ID '=' expression ';'
    | ID '[' expression ']' '=' expression ';'
    ;

expression
    : '(' expression ')' #Parenthesis
    | expression '[' expression ']' #ArrayAccess
    | expression '.' ID '(' ( expression ( ',' expression )* )? ')' #MethodAccess
    | 'new' ID '(' ')' #NewObject
    | 'new' 'int' '[' expression ']' #NewArray
    | expression op='.' 'length' #AccessOp
    | op='!' expression #UnaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('&&' | '<') expression #BinaryOp
    | 'true' #True
    | 'false' #False
    | 'this' #This
    | value=INTEGER #Integer
    | value=ID #Identifier
    ;
