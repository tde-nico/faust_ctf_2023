from Crypto.Cipher import AES
from Crypto.Util.Padding import pad
from PIL import Image
import numpy as np

# encrypts and decrypts images using AES in block cipher mode of operation
def convertImage(image, operation, key, initial_vector):

    # generate an AES cipher
    AES_obj = AES.new(key, AES.MODE_OFB, initial_vector)

    # extract image values
    image = np.array(image)
   
    # get image properties
    height, width, channels = image.shape

    # encrypt/decrypt image values
    if operation == "encrypt":
        converted_image_bytes = AES_obj.encrypt(image.tobytes()) 
    elif operation == "decrypt":
        converted_image_bytes = AES_obj.decrypt(image.tobytes()) 
    else:
        print("Error! Unknown operation for image conversion.")
        return None

    # reshape converted image values into image shape
    deserialized_bytes = np.frombuffer(converted_image_bytes, dtype=np.uint8)
    converted_image_data = np.reshape(deserialized_bytes, newshape=(height, width, channels))

    # return converted image
    return Image.fromarray(converted_image_data, mode='RGB')


# pads passwords to multiples of 16 bytes for AES
def sanitize_password(password):

    password = password[:16].encode()
    if len(password) % 16 != 0:
        password = pad(password, 16)
    return password