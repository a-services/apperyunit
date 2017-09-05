# Run ApperyUnit

AU="java -cp build/libs/apperyunit.jar io.appery.apperyunit.au"

case "$1" in
	"r" ) $AU helloappery.js
		;;
	"e" ) $AU helloappery.js echo
		;;
	"t" ) $AU helloappery.js test
		;;
	"d" ) $AU download
		;;
	* ) $AU 
		;;
esac	
