/**
 * Library used in "Hello, Appery.io!" server code example.
 * 
 * We export constants and functions as `auExample` module.
 */
var auExample = {

    /* Appery Database Id */
    dbId: '58ad4ff2e4b0e91ec571ce2d',

    /* Reports an error as HTTP 400 */
    errorMessage: function(message) {
        response.error({
            message: message
        }, 400);
    }

};