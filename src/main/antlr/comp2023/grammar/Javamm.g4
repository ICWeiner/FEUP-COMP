grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0-9]+ ;
ID : [a-zA-Z_][a-zA-Z_0-9]* ;
LineComment: '//' ~[\r\n]* -> skip;
Comment: '/*' .*? '*/' -> skip;
WS : [ \t\n\r\f]+ -> skip ;

//TODO:Review names of rules and fields here

program
    : (importDeclaration)* value=classDeclaration EOF #ImportStmt //TODO: name not too good
    | statement+ EOF #ProgramStmt //not needed?
    ;

importDeclaration
    : 'import' name+=ID ('.' name+=ID)* ';'
    ;

classDeclaration
    : 'class' name=ID ('extends' superName=ID)? '{' (varDeclaration)* (methodDeclaration)* '}' //TODO:Naming of fields might need revision
    ;

varDeclaration
    : kind=type name=ID ';'
    ;


methodDeclaration
    : ('public')? kind=type name=ID '(' (paramKind+=type paramName+=ID (',' paramKind+=type paramName+=ID)* )? ')' '{' (varDeclaration)* (statement)* 'return' returnType=expression ';' '}' #CustomMethod
    | ('public')? 'static' 'void' 'main' '(' value+=ID '[' ']' value+=ID ')' '{' (varDeclaration)* (statement)* '}' #MainMethod
    ;

type
    : 'int' '[' ']' //#IntArrayType//TODO: this adds clutter to visitor, rethink names here aswell
    | 'boolean' //#BooleanType
    | 'int' //#IntegerType
    | name=ID //#CustomType
    ;
/* //TODO: implement this better version
type locals[boolean isArray=false, boolean isPrimitive=true]
    : name = (INT | BOOLEAN) (LSQUARE RSQUARE {$isArray=true;})? #primitiveType
    | value=ID #CustomType
    ;*/

statement
    : '{' ( statement )* '}' #Curlys
    | 'if' '(' expression ')' statement 'else' statement #IfElseStmt
    | 'while' '(' expression ')' statement #WhileStmt
    | expression ';' #ExprStmt
    | name=ID '=' expression ';' #Assignment
    | name=ID '[' expression ']' '=' expression ';' #ArrayAssignment
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
