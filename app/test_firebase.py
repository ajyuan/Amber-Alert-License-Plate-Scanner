from firebase import firebase
import json
import random 

def randomLicense():
    numberPlate = "6"
    
    upperCaseLetters=chr(random.randint(65,90)) + chr(random.randint(65,90)) + chr(random.randint(65,90))
    numberPlate = numberPlate + upperCaseLetters + str(random.randint(0, 9)) + str(random.randint(0, 9)) + str(random.randint(0, 9))
    return numberPlate


firebase = firebase.FirebaseApplication('https://slugalert.firebaseio.com', None)

with open("./exampleStructs/names.json", 'r') as f:
    first_names = json.loads(f.read())

with open('./exampleStructs/exampleMockAmberAlert.json', 'r') as f:
    mockAmber = json.loads(f.read())

with open('./exampleStructs/images.json', 'r') as f:
    images_url = random.sample(json.loads(f.read()), 10)

with open('./exampleStructs/colors.json', 'r') as f:
    colors = list(json.loads(f.read()).values())

for i in range(10):
    amber = mockAmber.copy()
    amber['name'] = random.choice(first_names)
    amber['picture_url'] = images_url[i]
    amber['vehicle_information']['color'] = random.choice(colors)
    amber['vehicle_information']['license_plate'] = randomLicense()
    amber['eye_color'] = random.choice(colors)
    amber['hair_color'] = random.choice(colors)
    amber['age'] = random.randrange(1, 100)
    amber['height'] = "{}'{}".format(random.randrange(4, 6), random.randrange(0, 12))
    amber['weight'] = "{} Kg".format(random.randrange(30, 100))
    firebase.post('/MockAmberAlert', amber)

