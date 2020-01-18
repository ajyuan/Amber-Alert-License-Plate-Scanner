from firebase import firebase
firebase = firebase.FirebaseApplication('https://slugalert.firebaseio.com', None)


firebase.post('/MockAmberAlert', {'name':'John Doe', "age": 19, 'vehicle_information': {"license_plate": "XALDF", "color": "Blue"}})