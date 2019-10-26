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
GOO_TEXT_ANNOTATE_URL = 'https://labs.goo.ne.jp/api/entity'

def get_eye_point(res):
    landmarks = res['landmarks']
    eye_top_x = 0
    eye_top_y = 0
    eye_end_x = 0
    eye_end_y = 0
    for mark in landmarks:
        if mark['type'] == "LEFT_EYE_TOP_BOUNDARY":
            eye_top_y = int(mark['position']['y'])
        if mark['type'] == "LEFT_EYE_BOTTOM_BOUNDARY":
            eye_end_y = int(mark['position']['y'])
        if mark['type'] == "LEFT_EYE_RIGHT_CORNER":
            eye_end_x = int(mark['position']['x'])
        if mark['type'] == "LEFT_EYE_LEFT_CORNER":
            eye_top_x = int(mark['position']['x'])
    eye_top = (eye_top_x, eye_top_y)
    eye_end = (eye_end_x, eye_end_y)

# テキスト判別
def textAnalyze(text):
    text = text.replace(' ', '')
    response = requests.post("")

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

        # Storageから統計データを取得
        client = storage.Client()
        bucket = client.get_bucket('cras_storage')
        # https://console.cloud.google.com/storage/browser/[bucket-id]/
        db_blob = bucket.get_blob('durian_db.json')
        db_json = json.loads(db_blob.download_as_string().decode('utf-8'))
        if 'statistics' not in db_json:
            db_json['statistics'] = {}
        statistics_dict = db_json['statistics']

        # APIKey取得
        google_api_key = ''
        with open('google_api_key.txt', 'r') as txt:
            google_api_key = txt.read()
        goo_api_key = ''
        with open('goo_api_key.txt', 'r') as txt:
            goo_api_key = txt.read()
        # StoreageからAPIkeyを取得  
        # api_blob = bucket.get_blob('google_api_key.txt')
        # google_api_key = api_blob.download_as_string().decode('utf-8')

        # スキャンする画像を取得 -> ndarrayに変換
        image_requests = []
        ctxt = request_json['img']
        buf = b64decode(ctxt)
        nparr = np.frombuffer(buf, dtype=np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_ANYCOLOR)

        image_requests.append({
            'image': {'content': ctxt},
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

        # モザイクを入れる座標
        return_mosaic_list = []
        # 検出されたタグ
        detected_dict = {
            'face': False,
            'pupil': False,
            'finger': False,
            'text': False,
            'landmark': False
        }
        # アドバイス連想配列
        advice_dict = {}

        res_json = response.json()["responses"]
        img_ann = res_json[0]

        # 確認されたラベルリスト
        check_label_list = []
        # ラベル解析
        if 'labelAnnotations' in img_ann:
            for label_info in img_ann['labelAnnotations']:
                if label_info["score"] < 0.4:
                    continue
                desc = label_info["description"]
                if desc == "Selfie":
                    # 自撮り
                    check_label_list.append("Selfie")
                    # TODO: 瞳警戒
                    # TODO: 背景警戒
                    # TODO: 文字警告
                if desc == "V Sign":
                    # Vサイン
                    check_label_list.append("V Sign")
                    # TODO: 指紋警戒
                    detected_dict['finger'] = True
                if desc == "Finger":
                    check_label_list.append("Finger")
                    detected_dict['finger'] = True
                if desc == "Screen" or desc == "Laptop":
                    check_label_list.append("Screen")
                    # 画面
                    # TODO: 文字警告
                    print()


        if 'faceAnnotations' in img_ann:
            # 顔検出に引っかかったら統計データ更新
            detected_dict['face'] = True
            tmp_face_points_list =[]
            tmp_left_pupil_points_list = []
            tmp_right_pupil_points_list = []  
            max_size = 0

            # 顔の座標を取得
            for face_info in img_ann['faceAnnotations']:
                face_point = face_info['fdBoundingPoly']['vertices']
                top_x = face_point[0]['x'] if 'x' in face_point[0] else 0
                top_y = face_point[0]['y'] if 'y' in face_point[0] else 0
                end_x = face_point[2]['x'] if 'x' in face_point[2] else 0
                end_y = face_point[2]['y'] if 'y' in face_point[2] else 0
                size = (end_x - top_x) * (end_y - top_y)
                tmp_face_points_list.append({
                    "name": "face",
                    "top_x": top_x,
                    "top_y": top_y,
                    "end_x": end_x,
                    "end_y": end_y,
                    "width": 10,
                    "color": (0, 0, 255),
                    "size": size
                })
            
                # 顔の最大サイズを決める
                if (size > max_size):
                    max_size = size

                # 目の座標も取得
                detected_dict['pupil'] = True  
                landmarks = face_info['landmarks']
                l_eye_top_x = 0
                l_eye_top_y = 0
                l_eye_end_x = 0
                l_eye_end_y = 0
                r_eye_top_x = 0
                r_eye_top_y = 0
                r_eye_end_x = 0
                r_eye_end_y = 0
                for mark in landmarks:
                    if mark['type'] == "LEFT_EYE_TOP_BOUNDARY":
                        l_eye_top_y = int(mark['position']['y'])
                    if mark['type'] == "LEFT_EYE_BOTTOM_BOUNDARY":
                        l_eye_end_y = int(mark['position']['y'])
                    if mark['type'] == "LEFT_EYE_RIGHT_CORNER":
                        l_eye_end_x = int(mark['position']['x'])
                    if mark['type'] == "LEFT_EYE_LEFT_CORNER":
                        l_eye_top_x = int(mark['position']['x'])
                    if mark['type'] == "RIGHT_EYE_TOP_BOUNDARY":
                        r_eye_top_y = int(mark['position']['y'])
                    if mark['type'] == "RIGHT_EYE_BOTTOM_BOUNDARY":
                        r_eye_end_y = int(mark['position']['y'])
                    if mark['type'] == "RIGHT_EYE_RIGHT_CORNER":
                        r_eye_end_x = int(mark['position']['x'])
                    if mark['type'] == "RIGHT_EYE_LEFT_CORNER":
                        r_eye_top_x = int(mark['position']['x'])

                # 左目
                tmp_left_pupil_points_list.append({
                    "name": "pupil",
                    "top_x": l_eye_top_x,
                    "top_y": l_eye_top_y,
                    "end_x": l_eye_end_x,
                    "end_y": l_eye_end_y,
                    "width": 2,
                    "color": (57, 108, 236)
                })
                # 右目
                tmp_right_pupil_points_list.append({
                    "name": "pupil",
                    "top_x": r_eye_top_x,
                    "top_y": r_eye_top_y,
                    "end_x": r_eye_end_x,
                    "end_y": r_eye_end_y,
                    "width": 2,
                    "color": (57, 108, 236)
                })

            for i, tmp_face_point in enumerate(tmp_face_points_list):
                if (max_size * 0.6 > float(tmp_face_point['size']) or img.shape[0]*img.shape[1] / 100 > float(tmp_face_point['size'])):
                    # 最大の顔サイズの0.6倍より小さければモザイク対象 || 画像の1/100のサイズもない場合 
                    del tmp_face_point['size']
                    return_mosaic_list.append(tmp_face_point)
                else:
                    # 顔は対象外だが、自撮りの場合は瞳にモザイク
                    if 'Selfie' in check_label_list:
                        return_mosaic_list.append(tmp_left_pupil_points_list[i])
                        return_mosaic_list.append(tmp_right_pupil_points_list[i])

        if 'textAnnotations' in img_ann:
            for text_info in img_ann['textAnnotations'][:10]:
                if 'locale' in text_info:
                    desc = text_info['description']
                    desc = desc.replace(' ', '')
                    desc = desc.replace('　', '')
                    # TODO: 固有表現抽出
                    data = {
                        'app_id': goo_api_key,
                        'sentence': desc
                    }
                    response = requests.post(GOO_TEXT_ANNOTATE_URL, data=json.dumps(data).encode(), headers={'Content-Type': 'application/json'})
                    result_texts = response.json()['ne_list']
                    advice_dict["text"] = []
                    for char_info in result_texts:
                        if "ART" in char_info:
                            # TODO 人工物検出
                            advice_dict["text"].append("人工物：%s"%(char_info[0]))
                        elif "ORG" in char_info:
                            # TODO 組織名検出
                            advice_dict["text"].append("組織名：%s"%(char_info[0]))
                        elif "LOC" in char_info:
                            # TODO 場所情報
                            advice_dict["text"].append("場所：%s"%(char_info[0]))
                        elif "PSN" in char_info:
                            # TODO 人物名検出
                            advice_dict["text"].append("人物：%s"%(char_info[0]))
                else:
                    text_point = text_info['boundingPoly']['vertices']
                    top_x = text_point[0]['x'] if 'x' in text_point[0] else 0
                    top_y = text_point[0]['y'] if 'y' in text_point[0] else 0
                    end_x = text_point[2]['x'] if 'x' in text_point[2] else 0
                    end_y = text_point[2]['y'] if 'y' in text_point[2] else 0
                    return_mosaic_list.append({
                        "name": "text",
                        "top_x": top_x,
                        "top_y": top_y,
                        "end_x": end_x,
                        "end_y": end_y,
                        "width": 5,
                        "color": (51, 77, 234)
                    })
                # テキストに人名か場所があった場合（危険度：高）
                # if "PSN" in text_detected_list or "LOC" in text_detected_list:
                #     # 文字の座標を取得
                #     text_point = text_info['boundingPoly']['vertices']
                #     top_x = text_point[0]['x'] if 'x' in text_point[0] else 0
                #     top_y = text_point[0]['y'] if 'y' in text_point[0] else 0
                #     end_x = text_point[2]['x'] if 'x' in text_point[2] else 0
                #     end_y = text_point[2]['y'] if 'y' in text_point[2] else 0
                #     return_mosaic_list.append({
                #         "name": "text",
                #         "top_x": top_x,
                #         "top_y": top_y,
                #         "end_x": end_x,
                #         "end_y": end_y,
                #         "width": 5,
                #         "color": (51, 77, 234)
                #     })

                # if "ART" in text_detected_list or "ORG" in text_detected_list:
                #     # 文字の座標を取得
                #     text_point = text_info['boundingPoly']['vertices']
                #     top_x = text_point[0]['x'] if 'x' in text_point[0] else 0
                #     top_y = text_point[0]['y'] if 'y' in text_point[0] else 0
                #     end_x = text_point[2]['x'] if 'x' in text_point[2] else 0
                #     end_y = text_point[2]['y'] if 'y' in text_point[2] else 0
                #     return_mosaic_list.append({
                #         "name": "text",
                #         "top_x": top_x,
                #         "top_y": top_y,
                #         "end_x": end_x,
                #         "end_y": end_y,
                #         "width": 5,
                #         "color": (255, 77, 0)
                #     })

        # return_mosaic_listの整理（消す座標は消す）
        # if 'Selfie' in check_label_list:
        #     # 自撮りの場合
        #     # 最もサイズが大きい顔を決定
        #     index = 0
        #     big_size = 0
        #     for i, p_info in enumerate(return_mosaic_list):
        #         if 'face' == p_info['name']:
        #             size = (p_info['end_x'] - p_info['top_x']) * (p_info['end_y'] - p_info['top_y'])
        #             if size > big_size:
        #                 big_size = size
        #                 index = i
        #     del return_mosaic_list[index]
            
        #     for i, p_info in enumerate(return_mosaic_list):
        #         if 'face' == p_info['name']:
            

        # モザイク処理
        for mosaic_point in return_mosaic_list:
            img = cv2.rectangle(
                img, 
                (mosaic_point["top_x"], mosaic_point["top_y"]),
                (mosaic_point["end_x"], mosaic_point["end_y"]),
                mosaic_point['color'],
                mosaic_point["width"])
        
        # 統計データ更新
        db_json['statistics'] = statistics_dict
        db_blob.upload_from_string(json.dumps(db_json))

        # レスポンスデータ作成
        result, img_bytes = cv2.imencode('.jpg', img)
        json_data = {
            'img': b64encode(img_bytes).decode(),
            'statistics': statistics_dict,
            'mosaic_points': return_mosaic_list,
            'advice': advice_dict
        }

        # レスポンス
        res = helpers.make_response(json.dumps(json_data).encode(), 200)
        res.headers["Content-type"] = "application/json"
        return res

    else:
        return f'Privacy Scan'