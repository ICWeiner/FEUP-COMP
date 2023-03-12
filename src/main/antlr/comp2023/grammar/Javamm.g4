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
    : 'import' value+=ID ('.' value+=ID)* ';'
    ;

classDeclaration
    : 'class' value=ID ('extends' superValue=ID)? '{' (varDeclaration)* (methodDeclaration)* '}' //TODO:Naming of fields might need revision
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
