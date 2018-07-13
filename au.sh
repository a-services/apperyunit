# Run ApperyUnit

AU="java -cp apperyunit-1.01.jar io.appery.apperyunit.au"

case "$1" in
	"e" ) $AU helloappery.js echo
		;;
	"t" ) $AU helloappery.js test
		;;
	"w" ) $AU
		;;
	* ) $AU helloappery.js 
		;;
esac	
