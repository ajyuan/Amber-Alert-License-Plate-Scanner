from firebase import firebase
import json
firebase = firebase.FirebaseApplication('https://slugalert.firebaseio.com', None)

with open('./exampleStructs/exampleMockAmberAlert.json', 'r') as f:
    mockAmber = json.loads(f.read())
firebase.post('/MockAmberAlert', mockAmber)