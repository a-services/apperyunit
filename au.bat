@echo off
set AU=java -cp ../apperyunit/build/libs/apperyunit-0.9.jar io.appery.apperyunit.au

if "%1%"=="r" (
	%AU% helloappery.js
	goto end
) 

if "%1%"=="e" (
	%AU% helloappery.js echo 
	goto end
) 

if "%1%"=="t" (
	%AU% helloappery.js test
	goto end
) 

if "%1%"=="d" (
	%AU% download
	goto end
) 

%AU% 

:end
