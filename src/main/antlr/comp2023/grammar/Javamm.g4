grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;
LineComment: '//' ~[\r\n]* -> skip;
Comment: '/*' .*? '*/' -> skip;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDeclaration)* classDeclaration EOF #ImportStmt
    | statement+ EOF #ProgramStmt
    ;

importDeclaration
    : 'import' value+=ID ('.' value+=ID)* ';'
    ;

classDeclaration
    : 'class' value+=ID ('extends' value+=ID)? '{' (varDeclaration)* (methodDeclaration)* '}'
    ;

varDeclaration
    : type value=ID ';'
    ;

methodDeclaration
    : ('public')? type value+=ID '(' (type value+=ID (',' type value+=ID)* )? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}' #CustomMethod
    | ('public')? 'static' 'void' 'main' '(' value+=ID '[' ']' value+=ID ')' '{' (varDeclaration)* (statement)* '}' #MainMethod
    ;

type
    : 'int' '[' ']' #IntArrayType
    | 'boolean' #BooleanType
    | 'int' #IntegerType
    | value=ID #CustomType
    ;

statement
    : '{' ( statement )* '}' #Curlys
    | 'if' '(' expression ')' statement 'else' statement #IfElseStmt
    | 'while' '(' expression ')' statement #WhileStmt
    | expression ';' #ExprStmt
    | value=ID '=' expression ';' #Assignment
    | value=ID '[' expression ']' '=' expression ';' #ArrayAssignment
    ;

expression
    : '/*' (WS | ID | INTEGER)* '*/' #MultiLineComment
    | '//' (ID | INTEGER)* #LineComment
    | '(' expression ')' #Parenthesis
    | expression '[' expression ']' #ArrayAccess
    | expression '.' value=ID '(' ( expression ( ',' expression )* )? ')' #MethodAccess
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
