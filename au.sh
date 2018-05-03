# Run ApperyUnit

AU="java -cp apperyunit-1.00.jar io.appery.apperyunit.au"

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
