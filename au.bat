@echo off
set AU=java -cp apperyunit-1.00.jar io.appery.apperyunit.au

if "%1%"=="e" (
	%AU% helloappery.js echo 
	goto end
) 

if "%1%"=="t" (
	%AU% helloappery.js test
	goto end
) 

if "%1%"=="w" (
	%AU% 
	goto end
) 

%AU% helloappery.js

:end
