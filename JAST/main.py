# The model and part of code retrived from https://github.com/krasserm/face-recognition

import csv, cv2, os.path, json, requests, datetime, os, time
from firebase import firebase
from dlib import rectangle
import numpy as np
import face_recognition as fr
import matplotlib.pyplot as plt
import matplotlib.patches as patches

from model import create_model
from align import AlignDlib

class IdentityMetadata():
    def __init__(self, base, name, file):
        self.base = base
        self.name = name
        self.file = file

    def __repr__(self):
        return self.image_path()

    def image_path(self):
        return os.path.join(self.base, self.name, self.file)

def load_metadata(path):
    metadata = []
    for i in os.listdir(path):
        for f in os.listdir(os.path.join(path, i)):
            ext = os.path.splitext(f)[1]
            if ext.lower() == '.jpg' or ext.lower() == '.jpeg':
                metadata.append(IdentityMetadata(path, i, f))
    return np.array(metadata)

def face_locations(path):
    image = fr.load_image_file(path)
    return fr.face_locations(image)

def align_image(img, idx):
    return alignment.align(96, img, rectangle(idx[-1], idx[0], idx[1], idx[2]), landmarkIndices=AlignDlib.OUTER_EYES_AND_NOSE)

def distance(emb1, emb2):
    return np.sum(np.square(emb1 - emb2))

def load_image(path):
    img = cv2.imread(path, 1)
    return img[...,::-1]

def check_input(path):
    img = load_image(path)
    idx = face_locations(path)
    for i in idx:
        img = align_image(img, i)
        img = (img / 255.).astype(np.float32)
    return nn4_small2_pretrained.predict(np.expand_dims(img, axis=0))[0]
    
def search(emd):
    res = []
    for i, j in enumerate(embedded):
        tmp = distance(j, emd)
        if tmp < 1:
            res.append((i, tmp))
    maybe = min(res, key=lambda x: x[-1])
    return maybe

def fdb_write(strs):
    fdb.put("/","label_here",strs)

def get_url(url):
    headers = {"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"}
    page = requests.get(url, headers=headers)
    return page.content.decode("utf-8")

def get_url_raw(url):
    headers = {"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"}
    page = requests.get(url, headers=headers)
    return page.content

def save_url(url, path):
    with open(path, 'wb') as f:
        f.write(get_url_raw(url))
    return True

def reader_train_eval():
    csvs = fdb.get("/", "csv")
    return csvs[:-1].split(",")

def readers(save_path):
    rr = reader_train_eval()
    for i in range(len(rr)):
        rr[i] = rr[i].replace(" ", "%20")
        rr[i] = rr[i].replace(":", "%3A")
        rr[i] += ".jpg"
    if len(rr) > 1:
        web = "https://firebasestorage.googleapis.com/v0/b/qhacks-34e07.appspot.com/o/train%2F"
    else:
        web = "https://firebasestorage.googleapis.com/v0/b/qhacks-34e07.appspot.com/o/evaluate%2F"
    web_addr_half = [web+i for i in rr]
    token = [json.loads(get_url(i))["downloadTokens"] for i in web_addr_half]
    web_addr_half = [i+"?alt=media&token=" for i in web_addr_half]
    zipped = list(zip(web_addr_half, token))
    ready = [''.join(i) for i in zipped]
    st = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d-%H-%M-%S')
    os.makedirs(save_path+"\\"+st)
    res = []
    for i in ready:
        res.append(save_path+"\\"+st+"\\"+i.split("%2F")[-1].split("?")[0])
        save_url(i, save_path+"\\"+st+"\\"+i.split("%2F")[-1].split("?")[0])
    return res

if __name__ == "__main__":
    nn4_small2_pretrained = create_model()
    nn4_small2_pretrained.load_weights("weights/nn4.small2.v1.h5")
    fdb = firebase.FirebaseApplication("https://qhacks-34e07.firebaseio.com", None)
    alignment = AlignDlib("models/landmarks.dat")

    embedded = []

    while True:
        pre = fdb.get("/", "csv")
        if pre == "" or pre == None or fdb.get("/", "csv") == pre or fdb.get("/", "csv") == "" or fdb.get("/", "csv") == None:
            time.sleep(1)
        else:
            q = readers("new")
            if len(q) > 1:
                metadata = load_metadata("new")
                for i, m in enumerate(metadata):
                    img = load_image(m.image_path())
                    idx = face_locations(m.image_path())
                    plots = [align_image(img, i) for i in idx]
                    plots = [(i / 255.).astype(np.float32) for i in plots]
                    for j in plots:
                        embedded.append(nn4_small2_pretrained.predict(np.expand_dims(j, axis=0))[0])
            else:
                c = check_input(q[0])
                cs = search(c)
                fdb_write(cs)
