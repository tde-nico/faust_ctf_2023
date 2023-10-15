/**
 * @params {array} files List of file items
 * @return FileList
 */
function FileListItems(files) {
    var b = new ClipboardEvent("").clipboardData || new DataTransfer()
    for (var i = 0, len = files.length; i < len; i++) b.items.add(files[i])
    return b.files
}



function addThumb(dropzone, file) {

    /**
     *  ████████╗██╗  ██╗██╗   ██╗███╗   ███╗██████╗ ███╗  ██╗ █████╗ ██╗██╗     
     *  ╚══██╔══╝██║  ██║██║   ██║████╗ ████║██╔══██╗████╗ ██║██╔══██╗██║██║     
     *     ██║   ███████║██║   ██║██╔████╔██║██████╦╝██╔██╗██║███████║██║██║     
     *     ██║   ██╔══██║██║   ██║██║╚██╔╝██║██╔══██╗██║╚████║██╔══██║██║██║     
     *     ██║   ██║  ██║╚██████╔╝██║ ╚═╝ ██║██████╦╝██║ ╚███║██║  ██║██║███████╗
     *     ╚═╝   ╚═╝  ╚═╝ ╚═════╝ ╚═╝     ╚═╝╚═════╝ ╚═╝  ╚══╝╚═╝  ╚═╝╚═╝╚══════╝
     */

    // create Thumbnail
    let thumbnail = $("<div></div>").get(0);
    $(thumbnail).addClass("thumb");
    $(dropzone).append(thumbnail);
    thumbnail.dataset.label = file.name;



    /**
     *  ███╗   ███╗███████╗████████╗██╗  ██╗ █████╗ ██████╗  ██████╗
     *  ████╗ ████║██╔════╝╚══██╔══╝██║  ██║██╔══██╗██╔══██╗██╔════╝
     *  ██╔████╔██║█████╗     ██║   ███████║██║  ██║██║  ██║╚█████╗ 
     *  ██║╚██╔╝██║██╔══╝     ██║   ██╔══██║██║  ██║██║  ██║ ╚═══██╗
     *  ██║ ╚═╝ ██║███████╗   ██║   ██║  ██║╚█████╔╝██████╔╝██████╔╝
     *  ╚═╝     ╚═╝╚══════╝   ╚═╝   ╚═╝  ╚═╝ ╚════╝ ╚═════╝ ╚═════╝ 
     */


    /**
     * DELETE THUMBNAIL AND CORRESPONDING FILE FROM DROPZONE INPUT
     * 
     * @param {Int} index 
     */
    thumbnail.delete = function (index) {

        let dropzone = this.closest(".dropzone");

        let input = $(dropzone).find("input")[0];
        let list = Array.from(input.files);

        list.splice(index, 1);

        input.files = new FileListItems(list);

        // reset prompt if list empty
        if (list.length == 0) {
            $($(dropzone).find("span")[0]).removeClass("small_prompt");
        }

        $(this).remove();
    }



    /**
     *  ██╗     ██╗ ██████╗████████╗███████╗███╗  ██╗███████╗██████╗ 
     *  ██║     ██║██╔════╝╚══██╔══╝██╔════╝████╗ ██║██╔════╝██╔══██╗
     *  ██║     ██║╚█████╗    ██║   █████╗  ██╔██╗██║█████╗  ██████╔╝
     *  ██║     ██║ ╚═══██╗   ██║   ██╔══╝  ██║╚████║██╔══╝  ██╔══██╗
     *  ███████╗██║██████╔╝   ██║   ███████╗██║ ╚███║███████╗██║  ██║
     *  ╚══════╝╚═╝╚═════╝    ╚═╝   ╚══════╝╚═╝  ╚══╝╚══════╝╚═╝  ╚═╝
     */


    // add click listener to thumbnail
    $(thumbnail).click(function () {
        let index = Array.from($(dropzone).find(".thumb")).indexOf(this);
        this.delete(index);
    });







    // make prompt smaller
    $($(dropzone).find("span")[0]).addClass("small_prompt");


    // show thumbnail if file is image
    if (file.type.startsWith("image/")) {
        let reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = function () {
            $(thumbnail).css("background-image", `url('${reader.result}')`);
        };
    }

}







$(document).ready(function () {
    $(".dropzone").each(function () {





        /**
         *  ██████╗ ██████╗  █████╗ ██████╗ ███████╗ █████╗ ███╗  ██╗███████╗
         *  ██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚════██║██╔══██╗████╗ ██║██╔════╝
         *  ██║  ██║██████╔╝██║  ██║██████╔╝  ███╔═╝██║  ██║██╔██╗██║█████╗  
         *  ██║  ██║██╔══██╗██║  ██║██╔═══╝ ██╔══╝  ██║  ██║██║╚████║██╔══╝  
         *  ██████╔╝██║  ██║╚█████╔╝██║     ███████╗╚█████╔╝██║ ╚███║███████╗
         *  ╚═════╝ ╚═╝  ╚═╝ ╚════╝ ╚═╝     ╚══════╝ ╚════╝ ╚═╝  ╚══╝╚══════╝
         */


        // store dropzone
        const dropzone = this;


        /**
         *   █████╗ ████████╗████████╗██████╗ ██╗██████╗ ██╗   ██╗████████╗███████╗ ██████╗
         *  ██╔══██╗╚══██╔══╝╚══██╔══╝██╔══██╗██║██╔══██╗██║   ██║╚══██╔══╝██╔════╝██╔════╝
         *  ███████║   ██║      ██║   ██████╔╝██║██████╦╝██║   ██║   ██║   █████╗  ╚█████╗ 
         *  ██╔══██║   ██║      ██║   ██╔══██╗██║██╔══██╗██║   ██║   ██║   ██╔══╝   ╚═══██╗
         *  ██║  ██║   ██║      ██║   ██║  ██║██║██████╦╝╚██████╔╝   ██║   ███████╗██████╔╝
         *  ╚═╝  ╚═╝   ╚═╝      ╚═╝   ╚═╝  ╚═╝╚═╝╚═════╝  ╚═════╝    ╚═╝   ╚══════╝╚═════╝ 
         */

        dropzone.input = $(this).find("input")[0];
        dropzone.addbtn = $(this).find("span")[0]


        /**
         *   ██████╗ ███████╗████████╗████████╗███████╗██████╗ 
         *  ██╔════╝ ██╔════╝╚══██╔══╝╚══██╔══╝██╔════╝██╔══██╗
         *  ██║  ██╗ █████╗     ██║      ██║   █████╗  ██████╔╝
         *  ██║  ╚██╗██╔══╝     ██║      ██║   ██╔══╝  ██╔══██╗
         *  ╚██████╔╝███████╗   ██║      ██║   ███████╗██║  ██║
         *   ╚═════╝ ╚══════╝   ╚═╝      ╚═╝   ╚══════╝╚═╝  ╚═╝
         */


        // returns boolean if dropzone is enabled
        dropzone.isEnabled = function () {
            return !$(dropzone).hasClass("dropzone-disabled");
        }

        // get selected files
        dropzone.getFiles = function () {
            return this.input.files;
        }


        /**
         *  ███╗   ███╗███████╗████████╗██╗  ██╗ █████╗ ██████╗  ██████╗
         *  ████╗ ████║██╔════╝╚══██╔══╝██║  ██║██╔══██╗██╔══██╗██╔════╝
         *  ██╔████╔██║█████╗     ██║   ███████║██║  ██║██║  ██║╚█████╗ 
         *  ██║╚██╔╝██║██╔══╝     ██║   ██╔══██║██║  ██║██║  ██║ ╚═══██╗
         *  ██║ ╚═╝ ██║███████╗   ██║   ██║  ██║╚█████╔╝██████╔╝██████╔╝
         *  ╚═╝     ╚═╝╚══════╝   ╚═╝   ╚═╝  ╚═╝ ╚════╝ ╚═════╝ ╚═════╝ 
         */

        // disable the dropzone
        dropzone.disable = function () {
            $(dropzone).addClass("dropzone-disabled");
        }
        // enable the dropzone
        dropzone.enable = function () {
            $(dropzone).removeClass("dropzone-disabled");
        }


        /**
         *  ADDS FILES TO THE DROPZONE INPUT
         * 
         * @param {FileList} files 
         */
        dropzone.addFiles = function (files) {

            let input = $(dropzone).find("input")[0];

            let newList = Array.from(files);
            let oldList = Array.from(input.files);
            let list = newList.concat(oldList);
            input.files = new FileListItems(list);

            $.each(files, function (index, file) {
                addThumb(dropzone, file);
            });
        }


        /**
         *  ██╗     ██╗ ██████╗████████╗███████╗███╗  ██╗███████╗██████╗ 
         *  ██║     ██║██╔════╝╚══██╔══╝██╔════╝████╗ ██║██╔════╝██╔══██╗
         *  ██║     ██║╚█████╗    ██║   █████╗  ██╔██╗██║█████╗  ██████╔╝
         *  ██║     ██║ ╚═══██╗   ██║   ██╔══╝  ██║╚████║██╔══╝  ██╔══██╗
         *  ███████╗██║██████╔╝   ██║   ███████╗██║ ╚███║███████╗██║  ██║
         *  ╚══════╝╚═╝╚═════╝    ╚═╝   ╚══════╝╚═╝  ╚══╝╚══════╝╚═╝  ╚═╝
         */

        // define dragover event
        $(dropzone).on("dragover", function (e) {
            e.preventDefault();
            $(this).addClass("drag-over");
        });

        // define dragleave & dragend event
        $.each(["dragleave", "dragend"], function (index, event) {
            $(dropzone).on(event, function () {
                $(this).removeClass("drag-over");
            });
        });

        // handle file drop
        $(dropzone).on("drop", function (e) {
            e.preventDefault();
            if (dropzone.isEnabled() && e.originalEvent.dataTransfer.files.length) {
                dropzone.addFiles(e.originalEvent.dataTransfer.files);
            }
            $(this).removeClass("drag-over");
        });


        $(dropzone.addbtn).click(function () {
            if (dropzone.isEnabled()) {
                let dialog = $("<input></input>");
                dialog.attr("type", "file");
                dialog.attr("multiple", "true");
                dialog.trigger("click");
                dialog.change(function () {
                    if (this.files.length > 0) {
                        dropzone.addFiles(this.files);
                    }
                });
            }
        });








        // reload input to dropzone
        $.each(dropzone.input.files, function (index, file) {
            addThumb(dropzone, file);
        });






    });
});