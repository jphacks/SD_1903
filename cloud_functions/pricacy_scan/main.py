import os
from flask import escape, helpers
import json
import numpy as np
import cv2
import requests
from PIL import Image
from base64 import b64decode, b64encode

os.environ["GOOGLE_APPLICATION_CREDENTIALS"]='./TeamCras-1788b902cdf8.json' 
from google.cloud import storage

GOOGLE_IMG_ANNOTATE_URL = 'https://vision.googleapis.com/v1/images:annotate'

def privacy_scan(request):
    """Responds to any HTTP request.
    Args:
        request (flask.Request): HTTP request object.
    Returns:
        The response text or any set of values that can be turned into a
        Response object using
        `make_response <http://flask.pocoo.org/docs/1.0/api/#flask.Flask.make_response>`.
    """
    request_json = request.get_json()
    if request.args and 'message' in request.args:
        return request.args.get('message')
    elif request_json and 'message' in request_json:
        return request_json['message']
    elif request_json and 'img' in request_json:
        google_api_key = ''
        with open('google_api_key.txt', 'r') as txt:
            goo_api_key = txt.read()

        # Storageから統計データを取得
        client = storage.Client()
        bucket = client.get_bucket('cras_storage')
        # https://console.cloud.google.com/storage/browser/[bucket-id]/
        blob = bucket.get_blob('cras_storage')
        db_json = json.loads(blob.download_as_string().decode('utf-8'))
        if 'statistics' not in db_json:
            db_json['statistics'] = {}
        statistics_dict = db_json['statistics']

        # スキャンする画像を取得 -> ndarrayに変換
        image_requests = []
        ctxt = request_json['img']
        buf = b64decode(ctxt)
        nparr = np.frombuffer(buf, dtype=np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_ANYCOLOR)

        image_requests.append({
            'image': ctxt,
            'features': [
                {
                    'type': 'LABEL_DETECTION',
                    'maxResults': 30
                },
                {
                    'type': 'TEXT_DETECTION',
                    'maxResults': 10
                },
                {
                    'type': 'FACE_DETECTION'
                }, 
                {
                    'type': 'LANDMARK_DETECTION'
                },
                {
                    'type': 'SAFE_SEARCH_DETECTION'
                },
                {
                    'type': 'OBJECT_LOCALIZATION'
                },
                {
                    'type': 'WEB_DETECTION'
                }
            ]
        })

        response = requests.post(GOOGLE_IMG_ANNOTATE_URL,
                        data=json.dumps({"requests": image_requests}).encode(),
                        params={'key': google_api_key},
                        headers={'Content-Type': 'application/json'})

        res = helpers.make_response(json.dumps(response.json()).encode(), 200)
        res.headers["Content-type"] = "application/json"
        return res

    else:
        return f'Privacy Scan'