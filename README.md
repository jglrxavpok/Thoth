Thoth
==========

A conditional translation language.

## What is it for?

This language is first targeted at developers who want to localise text while
trying to respect as much as possible the grammar and the syntax of said language.


You can decide to add a 's' at the end of an English word if it is plural for instance.
Or change a 'le' by a 'la' depending on the gender of another word in French!

Syntax
=========

## Declaring a Thoth class
You need to first include the class header at the top of the file: ``class package.YourClassName``

This class header is actually optional, if you omit it, the class name will be the first 5 characters of the file.


## Declaring a function/translation
In order to declare a function, use the ``def`` keyword followed by the function name and, 
if present, the arguments and the return type as following:

```
def functionName(arg1, arg2):n
```

The ':n' defines the type, with 'n' meaning neutral. 
That way, you tell Thoth that the result of this function call is a neutral expression/word.

You can also define a function without arguments that way:

```
def functionName:n
```

And if the type is neutral, you can omit the type token:

```
def functionName
```

Next, we want to add code to the function (the body of the function), to do that, we add '=' at the end of the function declaration, 
preceding the actual code.

Example:

```
def helloWorld=Hello world!
```

Calling this function will return a neutral Translation object containing "Hello world!"
 

If you want to use multiple functions in the same file (which you'll probably want), 
you need to specify the end of the function by adding the ``[end]`` keyword (it will be omitted in the rest of 
explanation if we're only using a single function as an example):

```
def functionA=Hello from function A![end]

def functionB=Hello from function B![end]
```

## Using an argument inside a function body
By default, everything typed inside a function body, with the exception of the ``[end]`` keyword, 
is considered plain text and will be treated like it: no processing will go through it.

If you want to embed code and use arguments inside this body, you will need to put it between '|'. For example:

```
def echo(arg)=You said |arg|!
```

The 'echo' function will output "You said " and will concatenate the value of 'arg' to it.

For instance, calling ``echo("Hello")`` returns "You said Hello!"

WIP README
=========
Still a lot to write