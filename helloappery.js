/**
 * "Hello, Appery.io!" server code example.
 * 
 * Returns the list of rows from Appery DB collection according to
 * some search criteria.
 *
 * In our example `Students` collection has `courses` column of
 * `Array` type where student's courses are listed.
 * We are retrieving the list of students who are attending some course.  
 * Course Id should be passed to this server code function
 * as HTTP request parameter of `course`.
 */

/* `dbApiKey` variable stores Appery Database Id.
 * 
 * We define this Id in `au_example_library.js`
 * exported as `auExample` module
 * to use it from several server code scripts.
 */
var dbApiKey = auExample.dbId;

/* `errorMessage` is a function that we are
 * importing from `auExample` module
 */
var errorMessage = auExample.errorMessage;


/* We should wrap our code into a `function` to be able 
 * to use `return` statements in Appery server code. */
(function() {

    /* We read input parameters from HTTP request here. 
     */
    var course = request.get('course');
    
	// Converting string value to a number.
	var courseId = parseInt(course);

    // Verifying required parameters
    if (!courseId) {
        errorMessage("Missing required parameter");
        return;
    }

    /* Sending read request to `Students` collection.
     */
    var resultList = Collection.query(dbApiKey, "Students", {
        "criteria": {
            "courses": courseId
        }
    });

    // Printing message to console
    console.log('Greetings from Appery.io to ' + resultList.length + ' students attending course ' + courseId + '!');


    /* Returning HTTP 200 and JSON array with the resulting list of students.
     */
    Apperyio.response.success({
        "result": resultList
    }, "application/json");

})();