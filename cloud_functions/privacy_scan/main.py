import os
from flask import escape, helpers
import json
import numpy as np
import pandas as pd
import cv2
import requests
from PIL import Image
from base64 import b64decode, b64encode


os.environ["GOOGLE_APPLICATION_CREDENTIALS"]='./TeamCras-1788b902cdf8.json' 
from google.cloud import storage

GOOGLE_IMG_ANNOTATE_URL = 'https://vision.googleapis.com/v1/images:annotate'
GOO_TEXT_ANNOTATE_URL = 'https://labs.goo.ne.jp/api/entity'

# Storageから統計データを取得
client = storage.Client()
bucket = client.get_bucket('cras_storage')
# Storageからデータベース取得
db_blob = bucket.get_blob('durian_db.json')
db_json = json.loads(db_blob.download_as_string().decode('utf-8'))


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

# 統計データ更新
def statistics_update(statistics, data):
    # 統計データ更新
    if 'face' in statistics:
        statistics['face'] += 1 if data['face'] else 0
    else:
        statistics['face'] = 1 if data['face'] else 0
    if 'pupil' in statistics:
        statistics['pupil'] += 1 if data['pupil'] else 0
    else:
        statistics['pupil'] = 1 if data['pupil'] else 0
    if 'finger' in statistics:
        statistics['finger'] += 1 if data['finger'] else 0
    else:
        statistics['finger'] = 1 if data['finger'] else 0
    if 'text' in statistics:
        statistics['text'] += 1 if data['text'] else 0
    else:
        statistics['text'] = 1 if data['text'] else 0
    if 'landmark' in statistics:
        statistics['landmark'] += 1 if data['landmark'] else 0
    else:
        statistics['landmark'] = 1 if data['landmark'] else 0
    
    return statistics

# ラベル統計データの上位10
def get_savings_labels():
    if "savings_labels" not in db_json:
        db_json["savings_labels"] = {}
    savings_labels = db_json["savings_labels"]
    if len(savings_labels.keys()) == 0:
        return None
        
    df = pd.DataFrame({
        'label': list(savings_labels.keys()),
        'value': [savings_labels[key] for key in list(savings_labels.keys())]
    })
    df = df.sort_values('value', ascending=False).iloc[0:10]
    return df

# ラベル統計データ更新
def update_savings_labels(savings, labels):
    # TODO: 処理
    # key_list = list(detecting.keys())
    # for key in key_list:
    #     if key not in savings: 
    #         savings[key] = {}
    #         savings[key]["total"] = 0
    for label in labels:
        savings[label] = savings[label]+1 if label in savings else 1

    return savings
            
    

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
        if 'savings_labels' not in db_json:
            db_json['savings_labels'] = {}
        if db_json['savings_labels'] is None:
            db_json['savings_labels'] = {}
        savings_labels = db_json['savings_labels']

        # APIKey取得
        google_api_key = ''
        with open('google_api_key.txt', 'r') as txt:
            google_api_key = txt.read()
        goo_api_key = ''
        with open('goo_api_key.txt', 'r') as txt:
            goo_api_key = txt.read()

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
                    'maxResults': 30
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
        detected_tag_dict = {
            'face': False,
            'pupil': False,
            'finger': False,
            'text': False,
            'landmark': False
        }
        # アドバイス連想配列
        advice_list = []

        res_json = response.json()["responses"]
        img_ann = res_json[0]

        # 危険度が高いラベルリスト
        danger_labels = get_savings_labels()

        # 確認されたラベルリスト
        checked_labels = []
        # 危険度が高いラベルと一致したラベルリスト
        result_danger_labels = {}
        # 統計ラベルリスト
        statistics_labels = []
        # ラベル解析
        if 'labelAnnotations' in img_ann:
            for label_info in img_ann['labelAnnotations']:
                if label_info["score"] < 0.5:
                    continue
                
                desc = label_info["description"]
                if desc == "Selfie":
                    # 自撮り
                    checked_labels.append("Selfie")
                    # ラベル追加    
                    statistics_labels.append(desc)
                elif desc == "V Sign":
                    # Vサイン
                    checked_labels.append("V Sign")
                elif desc == "Finger":
                    checked_labels.append("Finger")
                    detected_tag_dict['finger'] = True
                    advice_list.append(["指紋", "指の指紋が読み取られ、悪用されるかもしれません"])
                elif desc == "Screen" or desc == "Display":
                    checked_labels.append("Screen")
                    advice_list.append(["画面", "画面に映る情報が個人の特定に繋がる可能性があります"])
                elif desc == "School Uniform" or desc == "Uniform":
                    checked_labels.append("Uniform")
                    advice_list.append(["制服", "制服から写真の地域がバレる可能性があります"])
                else:
                    # ラベル追加    
                    statistics_labels.append(desc)

            # 危険性が高いラベルが含まれているかチェック
            if danger_labels is not None:
                top_danger_labels = list(danger_labels["label"])
                top_danger_values = list(danger_labels["value"])
                for label, value in zip(top_danger_labels, top_danger_values):
                    if label in statistics_labels:
                        result_danger_labels[label] = {
                            "check": label in statistics_labels,
                            "value": value
                        }

        if 'faceAnnotations' in img_ann:
            # 顔検出に引っかかったら統計データ更新
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
                if (max_size * 0.5 > float(tmp_face_point['size']) or img.shape[0]*img.shape[1] / 100 > float(tmp_face_point['size'])):
                    # 最大の顔サイズの0.5 倍より小さければモザイク対象 || 画像の1/100のサイズもない場合 
                    del tmp_face_point['size']
                    return_mosaic_list.append(tmp_face_point)
                    detected_tag_dict['face'] = True
                else:
                    # 顔は対象外だが、自撮りの場合は瞳にモザイク（かつ、顔のサイズが画像の1/2以上）
                    if 'Selfie' in checked_labels and img.shape[0]*img.shape[1] / 2 > float(tmp_face_point['size']):
                        return_mosaic_list.append(tmp_left_pupil_points_list[i])
                        return_mosaic_list.append(tmp_right_pupil_points_list[i])
                        detected_tag_dict['pupil'] = True

        if 'textAnnotations' in img_ann:
            for text_info in img_ann['textAnnotations']:
                if 'locale' in text_info:
                    desc = text_info['description']
                    desc = desc.replace(' ', '')
                    desc = desc.replace('　', '')
                    # TODO: 固有表現抽出
                    data = {
                        'app_id': goo_api_key,
                        'sentence': desc
                    }
                    # goo apiを用いてテキスト解析
                    response = requests.post(GOO_TEXT_ANNOTATE_URL, data=json.dumps(data).encode(), headers={'Content-Type': 'application/json'})
                    result_texts = response.json()['ne_list']
                    advice_text_flags = []
                    for char_info in result_texts:
                        if "ART" in char_info:
                            # TODO 人工物検出
                            advice_text_flags.append("ART")
                            detected_tag_dict['text'] = True
                            advice_list.append(["人工物", "「%s」が写っています。個人特定に利用される可能性があります。"%(char_info[0])])
                        elif "ORG" in char_info:
                            # TODO 組織名検出
                            advice_text_flags.append("ORG")
                            detected_tag_dict['text'] = True
                            advice_list.append(["組織名", "「%s」が写っています。個人特定に利用される可能性があります。"%(char_info[0])])
                        elif "LOC" in char_info:
                            # TODO 場所情報
                            advice_text_flags.append("LOC")
                            detected_tag_dict['text'] = True
                            advice_list.append(["場所", "「%s」が写っています。場所が特定されないように注意しましょう。"%(char_info[0])])
                        elif "PSN" in char_info:
                            # TODO 人物名検出
                            advice_text_flags.append("PSN")
                            detected_tag_dict['text'] = True
                            advice_list.append(["名前", "人物名「%s」が写っていま。名前がバレていないか確認しましょう。"%(char_info[0])])
                    # advice_text = ""
                    # if "ART" in advice_text_flags:
                    #     advice_list.append(["人工物", "文字に人工物が含まれています"])
                    # if "ORG" in advice_text_flags:
                    #     advice_list.append(["組織名", "文字に組織名が含まれています"])
                    # if "LOC" in advice_text_flags:
                    #     advice_list.append(["場所", "場所を特定できる文字が写っています"])
                    # if "PSN" in advice_text_flags:
                    #     advice_list.append(["名前", "人名が写っています"])
                    # advice_list.append(["text", advice_text])
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
            
        if 'landmarkAnnotations' in img_ann:
            detected_tag_dict['landmark'] = True
            landmarks_ann = img_ann['landmarkAnnotations']
            for landmark in landmarks_ann[:1]:
                desc = landmark['description']
                location = (landmark['locations'][0]['latLng']['latitude'], landmark['locations'][0]['latLng']['longitude'])
            advice_list.append(["Landmark", "写真の場所は緯度%f 経度%f" % (location[0], location[1])])

# ----------------------------------

        # 統計データ更新
        if 'statistics' not in db_json:
            db_json['statistics'] = {}
        statistics_dict = db_json['statistics']
        statistics_dict = statistics_update(statistics_dict, detected_tag_dict)

        if True in list(detected_tag_dict.values()):
            # ラベル学習用データ更新
            savings_labels = update_savings_labels(savings_labels, labels=statistics_labels)


        # 統計データアップロード
        db_json['statistics'] = statistics_dict
        db_json['savings_labels'] = savings_labels
        db_blob.upload_from_string(json.dumps(db_json), content_type="application/json")

# ----------------------------------

        # モザイク処理
        for mosaic_point in return_mosaic_list:
            img = cv2.rectangle(
                img, 
                (mosaic_point["top_x"], mosaic_point["top_y"]),
                (mosaic_point["end_x"], mosaic_point["end_y"]),
                mosaic_point['color'],
                mosaic_point["width"])

        # アドバイス構文最終構築
        if detected_tag_dict['face']:
            advice_list.append(["顔","背景に人の顔があります。加工しましょう"])
        if detected_tag_dict['pupil']:
            advice_list.append(['瞳', '瞳に映る景色から住所を特定されるかもしれません'])

        if len(advice_list) == 0:
            advice_list.append(["検出なし", "この画像に危険性は見つかりませんでした"])

        # レスポンスデータ作成
        result, img_bytes = cv2.imencode('.jpg', img)
        json_data = {
            'img': b64encode(img_bytes).decode(),
            'statistics': statistics_dict,
            'checks': detected_tag_dict,
            'mosaic_points': return_mosaic_list,
            'danger_labels': result_danger_labels,
            'advice': advice_list
        }

        # レスポンス
        res = helpers.make_response(json.dumps(json_data).encode(), 200)
        res.headers["Content-type"] = "application/json"
        return res

    else:
        return f'Privacy Scan'