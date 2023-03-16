grammar Javamm;

@header {
    package pt.up.fe.comp2023;
}

INTEGER : [0] | [1-9][0-9]* ;
ID : [a-zA-Z_$][a-zA-Z_$0-9]* ;
LineComment: '//' ~[\r\n]* -> skip;
Comment: '/*' .*? '*/' -> skip;
WS : [ \t\n\r\f]+ -> skip ;


//TODO:Review names of rules and fields here

program
    : (importDeclaration | WS)* (classDeclaration | WS)* (statement | WS)* EOF
     //(importDeclaration)* classDeclaration EOF #ImportStmt //TODO: name not too good
    //| statement+ EOF #ProgramStmt //not needed?
    //|: (importDeclaration | WS)* (classDeclaration | WS)* (statement | WS)* EOF #EntireProgram TODO:Tentative program statement
    ;

importDeclaration
    : 'import' name+=ID ('.' name+=ID)* ';'
    ;

classDeclaration
    : 'class' name=ID ('extends' superName=ID)? '{' (varDeclaration)* (methodDeclaration)* '}' //TODO:Naming of fields might need revision
    ;

varDeclaration
    : type ';'
    ;

methodDeclaration
    : ('public')? type '(' (type (',' type )* )? ')' '{' (varDeclaration)* (statement)* 'return' expression ';' '}' #CustomMethod
    | ('public')? 'static' 'void' name='main' '(' value+=ID '[' ']' value+=ID ')' '{' (varDeclaration)* (statement)* '}' #MainMethod
    ;

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
    | expression op='.' 'length' #LenghtOp //#AccessOp
    | op='!' expression #UnaryOp
    | expression op=('*' | '/') expression #BinaryOp
    | expression op=('+' | '-') expression #BinaryOp
    | expression op=('&&' | '<') expression #BinaryOp
    | 'new' name=ID '(' ')' #GeneralDeclaration
    | 'new' 'int' '[' expression ']' #IntArrayDeclaration
    | value=('true' | 'false') #Boolean
    | 'this' #This
    | value=INTEGER #Integer
    | value=ID #Identifier
    ;
