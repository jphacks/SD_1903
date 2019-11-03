from base64 import b64encode, b64decode
import os
import io
import json
import requests
import numpy as np
import cv2
from PIL import Image
from flask import escape, helpers

os.environ["GOOGLE_APPLICATION_CREDENTIALS"]='./TeamCras-1788b902cdf8.json' 
from google.cloud import storage


# モザイク処理
def mosaic(img, scale=0.1):
    # 画像を scale (0 < scale <= 1) 倍にリサイズする。
    mosaiced = cv2.resize(img, dsize=None, fx=scale, fy=scale, interpolation=cv2.INTER_NEAREST)
    # 元の大きさにリサイズする。
    h, w = img.shape[:2]
    mosaiced = cv2.resize(mosaiced, dsize=(w, h), interpolation=cv2.INTER_NEAREST)
    return mosaiced

# モザイク処理
def mosaic_dsize(img, scale=0.1):
    # 画像を scale (0 < scale <= 1) 倍にリサイズする。
    h, w = img.shape[:2]
    mosaiced = cv2.resize(img, dsize=(2 if int(w*scale) < 1 else int(w*scale), 2 if int(h*scale) < 1 else int(h*scale)), interpolation=cv2.INTER_NEAREST)
    # 元の大きさにリサイズする。
    
    mosaiced = cv2.resize(mosaiced, dsize=(w, h), interpolation=cv2.INTER_NEAREST)
    return mosaiced

# スタンプ処理
def stamp_replace(img, stamp):
    h, w = img.shape[:2]
    stamp = cv2.resize(stamp, dsize=(w, h), interpolation=cv2.INTER_NEAREST)
    return stamp


def mosaic_process(request):
    """Responds to any HTTP request.
    Args:
        request (flask.Request): HTTP request object.
    Returns:
        The response text or any set of values that can be turned into a
        Response object using
        `make_response <http://flask.pocoo.org/docs/1.0/api/#flask.Flask.make_response>`.
    """
    # Storageから統計データを取得
    client = storage.Client()
    bucket = client.get_bucket('cras_storage')
    jphacks_logo_blob = bucket.get_blob('jphacks_logo.png')
    # img_binarystream = io.BytesIO(jphacks_logo_blob.download_as_string())
    # img_pil = Image.open(img_binarystream)
    # img_numpy = np.asarray(img_pil)
    # stamp = cv2.cvtColor(img_numpy, cv2.COLOR_RGBA2BGR)
    stamped_nparr = np.frombuffer(jphacks_logo_blob.download_as_string(), dtype=np.uint8)
    stamp = cv2.imdecode(stamped_nparr, cv2.IMREAD_ANYCOLOR)

    request_json = request.get_json()
    if request.args and 'message' in request.args:
        return request.args.get('message')
    elif request_json and 'message' in request_json:
        return request_json['message']

    elif request_json and 'img' in request_json:
        # リクエストボディから画像データ取得
        image_requests = []
        ctxt = request_json['img']
        buf = b64decode(ctxt)
        nparr = np.frombuffer(buf, dtype=np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_ANYCOLOR)
        
        stamped_img = img.copy()

        # モザイク座標を取得
        points = request_json['mosaic_points']
        for point_info in points:
            name = point_info['name']
            top_x = point_info['top_x']
            top_y = point_info['top_y']
            end_x = point_info['end_x']
            end_y = point_info['end_y']

            # モザイク処理
            if name == 'face':
                if end_y - top_y > 100 and end_x - top_x > 100:
                    img[top_y: end_y, top_x: end_x] = mosaic(img[top_y: end_y, top_x: end_x], scale=0.1)
                else:
                    img[top_y: end_y, top_x: end_x] = mosaic(img[top_y: end_y, top_x: end_x], scale=0.1)

                stamped_img[top_y: end_y, top_x: end_x] = stamp_replace(stamped_img[top_y: end_y, top_x: end_x], stamp)

            if name == 'pupil':
                img[top_y: end_y, top_x: end_x] = mosaic(img[top_y: end_y, top_x: end_x], scale=0.5)
                stamped_img[top_y: end_y, top_x: end_x] = mosaic(stamped_img[top_y: end_y, top_x: end_x], scale=0.5)

            if name == 'text':
                if end_y - top_y <= 50 and end_x - top_x <= 50:
                    img[top_y: end_y, top_x: end_x] = mosaic_dsize(img[top_y: end_y, top_x: end_x], scale=0.2)
                    stamped_img[top_y: end_y, top_x: end_x] = mosaic_dsize(stamped_img[top_y: end_y, top_x: end_x], scale=0.2)
                else:
                    img[top_y: end_y, top_x: end_x] = mosaic_dsize(img[top_y: end_y, top_x: end_x], scale=0.1)
                    stamped_img[top_y: end_y, top_x: end_x] = mosaic_dsize(stamped_img[top_y: end_y, top_x: end_x], scale=0.1)

        # ndarrayをエンコード
        result, img_bytes = cv2.imencode('.jpg', img)
        stamp_result, stamp_img_bytes = cv2.imencode('.jpg', stamped_img)

        json_data = {
            'img': b64encode(img_bytes).decode(),
            'stamp_img': b64encode(stamp_img_bytes).decode()
        }

        res = helpers.make_response(json.dumps(json_data).encode(), 200)
        res.headers["Content-type"] = "application/json"
        return res

    else:
        return f'Mosaic Process'