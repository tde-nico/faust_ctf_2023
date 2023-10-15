/**
 * @param {HTMLElement} element The button element is used to get the form and formdata
 * @param {function} onSuccess The function is called with the response object as the parameter in case of success
 * @param {function} onError The function is called with the error as the parameter in case of an error
 * @param {function} onProgress This function is called when a progress was made with the current progress from 0 to one as the parameter
 * 
 * @return returns the request object
 * 
 * As a second parameter you can receive the element back in all functions
 */
function uploadDZ(element, onSuccess = function () { }, onError = function () { }, onProgress = function () { }) {

    // prevent form sending
    event.preventDefault();

    // get form
    let form = element.closest("form");

    // add enable function to form
    form.enable = function () {
        // enable all form elements
        $(form).find("*").each(function () {
            this.disabled = false;
        });


        /****************************
         *  DROPZONE SPECIFIC START
         ****************************/

        // disable dropzones
        $(form).find(".dropzone").each(function () {
            this.enable();
        });

        /****************************
         *  DROPZONE SPECIFIC END
         ****************************/
    }

    // get destination url
    let url = new URL(form.action);


    // disable all form elements
    $(form).find("*").each(function () {
        this.disabled = true;
    });


    /****************************
     *  DROPZONE SPECIFIC START
     ****************************/

    // disable dropzones
    $(form).find(".dropzone").each(function () {
        this.disable();
    });

    /****************************
     *  DROPZONE SPECIFIC END
     ****************************/


    // prepare form data
    let data = new FormData(form);


    // append file inputs to data
    $(form).find("input").each(function () {

        // get name
        let name = this.name;

        // treat if name is set
        if (name) {
            switch (this.type) {
                case "file":
                    // append all files
                    $.each($(this)[0].files, function () {
                        data.append(name, this);
                    });
                    break

                case "checkbox":
                    data.append(name, this.checked);
                    break;

                default:
                    data.append(name, $(this).val());
                    break;
            }
        }
    });


    // append textareas
    $(form).find("textarea").each(function () {
        // get name
        let name = this.name;
        if (name) {
            data.append(name, $(this).text());
        }
    });

    // append select
    $(form).find("select").each(function () {
        // get name
        let name = this.name;
        content = [];
        if (name) {
            $(this).find("option:selected").each(function () {
                content.push($(this).val());
            });
            data.append(name, JSON.stringify(content));
        }
    });

    // create request
    let request = $.ajax({
        type: 'POST',
        url: url.pathname,
        data: data,
        contentType: false,
        processData: false,
        dataType: 'json',
        // handle error
        error: function (req, status, error) {
            // enable form
            form.enable();
            // call user defined error handler
            onError(error, element);
        },
        // handle success
        success: function (response) {
            // enable form
            form.enable();
            // call user defined done function
            onSuccess(response, element);
        },
        // handle progress
        xhr: function () {
            var xhr = new window.XMLHttpRequest();
            //Upload progress
            xhr.upload.addEventListener("progress", function (evt) {
                if (evt.lengthComputable) {
                    onProgress(evt.loaded / evt.total, element);
                }
            }, false);
            return xhr;
        }
    });

    return request;
}