grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0] | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;
LineComment: '//' ~[\r\n]* -> skip;
Comment: '/*' .*? '*/' -> skip;
WS : [ \t\n\r\f]+ -> skip ;


//TODO:Review names of rules and fields here --> Looks better now?

program
    : (importDeclaration | WS)* (classDeclaration | WS)* (statement | WS)* EOF //dont particularly like statement part, but it wont pass a tutorial test withoutit
    ;

importDeclaration
    : 'import' name+=ID ('.' name+=ID)* ';'
    ;

classDeclaration
    : 'class' name=ID ('extends' superName=ID)? '{' (varDeclaration)* (methodDeclaration)* '}'
    ;

varDeclaration
    : type ';'
    ;

methodDeclaration
    : ('public')? type '(' (type (',' type )* )? ')' '{' (varDeclaration | statement)* returnDeclaration '}' #CustomMethod
    | ('public')? 'static' 'void' name='main' '(' value+=ID '[' ']' value+=ID ')' '{' (varDeclaration | statement)* '}' #MainMethod
    ;

returnDeclaration
    : 'return' expression ';';

type locals [boolean isArray = false]
    : typeName='int' ('[' ']'{$isArray = true;})? name=ID //#IntType
    | typeName='boolean' name=ID //#BooleanType
    | typeName=ID name=ID //#CustomType
    ;

statement
    : '{' ( statement )* '}' #BlockStatement
    | 'if' '(' expression ')' statement 'else' statement #IfElseStmt
    | 'while' '(' expression ')' statement #WhileStmt
    | expression ';' #ExprStmt
    | name=ID '=' expression ';' #Assignment
    | name=ID '[' expression ']' '=' expression ';' #ArrayAssignment
    ;

expression
    : '(' expression ')' #Parenthesis
    | expression '[' expression ']' #ArrayAccess
    | expression '.' value=ID '(' ( expression ( ',' expression )* )? ')' #MethodCall
    | expression op='.' 'length' #LengthOp //#AccessOp
    | op='!' expression #UnaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op='<' expression #BinaryOp
    | expression op='&&' expression #BinaryOp
    | 'new' name=ID '(' ')' #GeneralDeclaration
    | 'new' 'int' '[' expression ']' #IntArrayDeclaration
    | value=('true' | 'false') #Boolean
    | 'this' #This
    | value=INTEGER #Integer
    | value=ID #Identifier
    ;