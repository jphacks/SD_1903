from base64 import b64encode, b64decode
import io
import json
import requests
import numpy as np
import cv2
from PIL import Image
from flask import escape, helpers


# モザイク処理
def mosaic(img, scale=0.1):
    # 画像を scale (0 < scale <= 1) 倍にリサイズする。
    mosaiced = cv2.resize(img, dsize=None, fx=scale, fy=scale, interpolation=cv2.INTER_NEAREST)
    # 元の大きさにリサイズする。
    h, w = img.shape[:2]
    mosaiced = cv2.resize(mosaiced, dsize=(w, h), interpolation=cv2.INTER_NEAREST)
    return mosaiced

def mosaic_dsize(img, scale=0.1):
    # 画像を scale (0 < scale <= 1) 倍にリサイズする。
    h, w = img.shape[:2]
    mosaiced = cv2.resize(img, dsize=(2 if int(w*scale) < 1 else int(w*scale), 2 if int(h*scale) < 1 else int(h*scale)), interpolation=cv2.INTER_NEAREST)
    # 元の大きさにリサイズする。
    
    mosaiced = cv2.resize(mosaiced, dsize=(w, h), interpolation=cv2.INTER_NEAREST)
    return mosaiced


def mosaic_process(request):
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
        # リクエストボディから画像データ取得
        image_requests = []
        ctxt = request_json['img']
        buf = b64decode(ctxt)
        nparr = np.frombuffer(buf, dtype=np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_ANYCOLOR)

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
            if name == 'pupil':
                img[top_y: end_y, top_x: end_x] = mosaic(img[top_y: end_y, top_x: end_x], scale=0.5)
            if name == 'text':
                if end_y - top_y <= 50 and end_x - top_x <= 50:
                    img[top_y: end_y, top_x: end_x] = mosaic_dsize(img[top_y: end_y, top_x: end_x], scale=0.2)
                else:
                    img[top_y: end_y, top_x: end_x] = mosaic_dsize(img[top_y: end_y, top_x: end_x], scale=0.1)

        
        result, img_bytes = cv2.imencode('.jpg', img)

        json_data = {
            'img': b64encode(img_bytes).decode()
        }

        res = helpers.make_response(json.dumps(json_data).encode(), 200)
        res.headers["Content-type"] = "application/json"
        return res

    else:
        return f'Mosaic Process'