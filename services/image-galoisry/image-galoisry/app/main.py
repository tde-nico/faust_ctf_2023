import os
import io
import re
import time
from flask import Flask, render_template, request, redirect, url_for, flash, session, send_file
import logging
from flask_session import Session
import uuid
import pymongo
from imagecrypto import convertImage, sanitize_password
from PIL import Image, UnidentifiedImageError
import numpy as np
import atexit
from apscheduler.schedulers.background import BackgroundScheduler

# set gallery path
upload_path = "static/uploads/"

# limit resolution (we can offer 4k support for paid plans in the future, hehe!)
max_resolution = 1280*800*3

# limit maximum number of parallel uploads
max_number_uploads = 5

# limit time images can stay on the server - this should keep our memory usage under control hopefully
time_threshold = 20 * 60  # 20 minutes * 60 seconds/minute

# configure mongodb
db = pymongo.MongoClient("mongodb://root:example@mongo:27017")["service"]
galleries = db["galleries"]
galleries.create_index([('name', 1)], unique=True)

# configure flask
app = Flask(__name__)
app.config['SESSION_TYPE'] = 'filesystem'
app.config['SECRET_KEY'] = str(uuid.uuid4())

# create a flask session
server_session = Session(app)

# configure log output
logging.basicConfig(level=logging.INFO) #.DEBUG)

@app.before_request
def log_request_info():
    app.logger.debug('Headers: %s', request.headers)
    app.logger.debug('Body: %s', request.get_data())


# ensures users cannot inject malicous stuff in input forms
def sanitize_input(filename):
    allowed_characters = r'[\w\s\-\.]'
    sanitized_filename = ''.join(re.findall(allowed_characters, filename))
    return sanitized_filename

# compares user input with saved password in db
def password_valid(gallery_name, user_input):

    # get gallery entry from db via name
    gal = galleries.find_one({'name': gallery_name})
    if(not gal): return False

    # get saved password from db
    gallery_password = gal.get('password')

    # compare user input with password
    if user_input == gallery_password:
        return True
    else:
        flash("Incorrect password", 'error')
        return False


# creates a gallery entry in database and a directory for upload
def create_gallery(name, description, password):
    
    # sanitize user input
    name = sanitize_input(name)
    password = sanitize_password(password)
    description = sanitize_input(description)

    try:
        # create db entry
        galleries.insert_one({'name': name, 'description': description, 'password': password})
        
        # create directory
        os.makedirs(f'static/uploads/{name}', exist_ok=True)
        
        # logging
        app.logger.info("Created new gallery:" + str(name))

        # reload main page
        return redirect(url_for('index'))

    except pymongo.errors.DuplicateKeyError: # ensure gallery name is unique
        flash("Gallery already exists!", 'error')
        return redirect(url_for('create'))

# loads all gallery entries from database
def get_galleries():
    gals = galleries.find({}, {'name': 1, 'description': 1})
    app.logger.info("Get galleries apicall, returned: " + str(gals))
    return gals

# deletes a gallery from database
def deleteGallery(gallery_name):
    response = galleries.delete_one({"name": gallery_name})
    app.logger.info("Delete gallery apicall, returned: " + str(response))

# deletes image files and galleries older than a specified time
def delete_old_files():

    # get current time in seconds since the epoch
    current_time = time.time()
 
    # get all gallery upload directories
    gallerie_paths = [f.path for f in os.scandir(upload_path) if f.is_dir()]

    # check all images in all directories
    for gal in gallerie_paths:
        for image in os.listdir(gal):

            # get file path
            file_path = os.path.join(gal, image)

            # check if file exists and is a regular file (not a directory)
            if os.path.isfile(file_path):
                
                # check if the image is too old
                if (current_time - os.path.getatime(file_path))> time_threshold:
                    
                    try: # delete the image file
                        os.remove(file_path)
                        app.logger.info("Successfully deleted old image file: " + str(file_path))
                    except Exception as e:
                        app.logger.info("Error deleting old image file" + str(file_path) + ": " + str(e))
        
        # check if directory is too old and empty
        if (current_time - os.path.getatime(gal)) > time_threshold and len(os.listdir(gal)) == 0:
            
            try:
                # delete database entry
                deleteGallery(gal.split('/')[-1])
                
                # delete directory
                os.rmdir(gal)
                app.logger.info("Successfully deleted old, empty gallery: " + str(gal))
            except Exception as e:
                app.logger.info("Error deleting empty gallery: " + str(gal))


####################
# ROUTES FOR FLASK #
####################

# route for main page: renders index.html and shows all existing galleries
@app.route("/")
def index():
    return render_template("index.html", galleries=get_galleries())

# route for gallery creation form
@app.route("/create", methods=['GET', 'POST'])
def create():

    # render the gallery creation form in case of GET request
    if request.method == 'GET':
        return render_template("create.html")

    # check user input and create gallery in case of POST request
    if request.method == 'POST':
        
        # extract user input
        name = request.form.get("gallery_name", False)
        description = request.form.get("description", False)
        password = request.form.get("password", False)

        # check length of gallery name
        if len(name) < 2 or len(name) > 32:
            flash("Gallery creation failed: Use between 2 and 32 characters for the gallery name!", 'error')
            app.logger.info("Gallery creation failed: Gallery name too short or long")
            return render_template("create.html")

        # check length of gallery description
        if len(description) > 2000:
            flash("Gallery creation failed: Use not more than 2000 characters for the gallery description!", 'error')
            app.logger.info("Gallery creation failed: Gallery description too long")
            return render_template("create.html")

        # check length of password
        if len(password) < 6 or len(password) > 32:
            flash("Gallery creation failed: Use between 6 and 32 characters for the gallery password!", 'error')
            app.logger.info("Gallery creation failed: Gallery password too short or too long")
            return render_template("create.html")

        # create gallery
        return create_gallery(name, description, password)

# route for gallery view: shows buttons to upload images and delete gallery and lists all uploaded images
@app.route('/gallery/<gallery_name>', methods=['GET'])
def gallery(gallery_name):

    # retrieve gallery entry from database via name
    gallery = galleries.find_one({'name': gallery_name})
    
    # check if gallery exists    
    if gallery:

        # extract all png files in corresponding upload directory
        images = []
        for filename in os.listdir(f'static/uploads/{gallery_name}'):
            if filename.lower().endswith(('.png')):
                images.append(filename)
            
        # render gallery view with list of found images
        return render_template('gallery.html', gallery=gallery_name, description=gallery['description'], images=images)

    else:
        return "Gallery not found.", 404

# route for image upload widget
@app.route('/gallery/<gallery_name>/upload', methods=['GET', 'POST'])
def gallery_upload(gallery_name):

    # retrieve gallery entry from database via name
    gallery = galleries.find_one({'name': gallery_name})

    # check if gallery exists
    if not gallery:
        return "Gallery not found.", 404
    
    # render upload widget in case of a GET request
    if request.method == 'GET':
        return render_template('upload.html', gallery=gallery_name)

    # encrypt and upload images in case of a POST request
    if request.method == 'POST':
        
        # extract files uploaded by user
        files = request.files.getlist('mediafile')

        # retrieve the secret gallery password from database
        gallery_password = gallery.get('password')

        # check if user has exceeded maximum number of uploads
        if len(files) > max_number_uploads:
            flash("Image upload failed: Don't upload more than 5 images at the same time!", "error")
            return redirect(url_for('gallery', gallery_name=gallery_name))

        # loop over all uploaded files
        upload_problems = False
        for file in files:
       
            if file:

                # sanitize file name first (never trust our users!)
                file_name = sanitize_input(file.filename)

                # check the file extension for known image formats
                if file_name.split(".")[1] not in ['jpg', 'jpeg', 'png', 'bmp', 'tif', 'tiff']:
                    flash("Use only known image formats with jpg/jpeg/png/bmp/tif/tiff file extensions!", "error")
                    upload_problems = True
                    continue

                # generate path and file name of uploaded file
                encrypted_file_name = file_name.split(".")[0]+".png"
                filename_path = os.path.join(f'static/uploads/{gallery_name}', encrypted_file_name)
                
                # ensure file has not yet been uploaded yet
                if encrypted_file_name in os.listdir(f'static/uploads/{gallery_name}'):
                    flash("File name already exists in gallery. Please rename image file!","error")
                    upload_problems = True
                    continue

                # try if the uploaded files are really images (didn't we check the file extension yet?)
                try:
                    image = Image.open(file)
                except UnidentifiedImageError as e:
                    flash("Unable to open uploaded image due to unknown image format.","error")
                    upload_problems = True
                    continue

                # ensure the uploaded image does not exceed the specified image resolution
                if np.prod(image.size) > max_resolution:
                    flash("Uploaded image too big. Please rescale RGB images to have a resolution of maximum 1280x800 pixels!","error")
                    upload_problems = True
                    continue
                
                # encrypt and upload image to server
                encrypted_image = convertImage(image, "encrypt", gallery_password, sanitize_password(encrypted_file_name))
                app.logger.info("Uploaded image: " + str(encrypted_file_name))
                encrypted_image.save(filename_path)
                
        # generate some fancy output to the user
        if upload_problems:
            flash("Image upload (partially) failed.","error")
        else:
            flash("Successfully uploaded images.","success")

        # return to the gallery view
        return redirect(url_for('gallery', gallery_name = gallery_name))


# route for image deletion
@app.route('/gallery/<gallery_name>/delete', methods=['POST'])
def delete_file(gallery_name):

    # extract user input
    data = request.json 
    file_name = data.get('fileId')

    # sanitize user input
    file_name = sanitize_input(file_name)
    password = sanitize_password(data.get('password'))

    # compare user password with secret gallery password
    if not password_valid(gallery_name, password):
        return "Incorrect password!", 403
    
    # delete the image file if password is correct
    file_path = f'static/uploads/{gallery_name}/{file_name}'
    if os.path.exists(file_path):
        os.remove(file_path)
        flash("Successfully deleted image","success")

        # return to the gallery view
        return redirect(url_for('gallery', gallery_name=gallery_name))
    else:
        return f'The file {file_path} does not exist', 404

# route for decrypting an image
@app.route('/gallery/<gallery_name>/decrypt', methods=['POST'])
def decrypt_file(gallery_name):

    # extract user input
    data = request.json
    file_name = data.get('fileId')

    # sanitize user input
    file_name = sanitize_input(file_name)
    password = sanitize_password(data.get('password'))
    
    # generate file path
    file_path = f'static/uploads/{gallery_name}/{file_name}'

    # check if file exists on server
    if os.path.exists(file_path):

        # open image for reading
        image = Image.open(file_path, 'r')

        # decrypt file using user password
        decrypted_image = convertImage(image, "decrypt", password, sanitize_password(file_name))
        
        # create an output buffer and write decrypted image to it
        img_io = io.BytesIO()
        decrypted_image.save(img_io, 'PNG')
        img_io.seek(0)

        # send file to user
        return send_file(img_io, as_attachment=True, download_name=file_name, mimetype='image/png')
    else:
        return f'The file {file_path} does not exist', 404


# route for downloading an encrypted image
@app.route('/gallery/<gallery_name>/download/<file_name>', methods=['GET'])
def download_file(gallery_name, file_name):

    # sanitize user input
    file_name = sanitize_input(file_name)
    
    # generate file path
    file_path = f'static/uploads/{gallery_name}/{file_name}'

    # check if file exists
    if os.path.exists(file_path):
        
        # send incrypted image file to user
        return send_file(file_path, as_attachment=True)
    
    else:
        flash('File does not exist', 'error')
        return redirect(url_for('gallery', gallery_name=gallery_name))




# route for deleteing a gallery
@app.route('/gallery/<gallery_name>/delete_gal', methods=['POST'])
def delete_gal(gallery_name):

    # sanitize user input
    password = sanitize_password(request.json.get('password'))
    
    # generate gallery directory path
    gallery_path = f'static/uploads/{gallery_name}'

    # compare user password with secret gallery password
    if not password_valid(gallery_name, password):
        return redirect(url_for('gallery', gallery_name=gallery_name))
    
    # check if directory for gallery exists
    if os.path.exists(gallery_path):

        # loop over all files in directory
        for image in os.listdir(gallery_path):

            # create file path
            file_path = os.path.join(gallery_path, image)
            try: # try to delete the image
                os.remove(file_path)
                app.logger.info("Successfully deleted image file: " + str(file_path))
            except Exception as e:
                app.logger.info("Error deleting image file" + str(file_path) + ": " + str(e))
        
        # delete database entry for gallery
        deleteGallery(gallery_name)

        # delete gallery directory
        try:
            os.rmdir(gallery_path)
            app.logger.info(f"Gallery directory '{gallery_path}' deleted successfully.")
        except OSError as e:
            app.logger.info(f"Error deleting gallery directory: {e}")

    # return to main page
    return redirect(url_for('index'))
    

###############
# MAIN SCRIPT #
###############    

if __name__ == '__main__':

    # run a scheduler to delete stale images so that we don't need to pay beefy fees for extra memory again
    scheduler = BackgroundScheduler()
    scheduler.add_job(func=delete_old_files, trigger="interval", seconds=60)
    scheduler.start()
    atexit.register(lambda: scheduler.shutdown())

    # start flask app
    app.run(host="::")
    app.logger.info("Off we go!")
