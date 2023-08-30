import json, pytz
from datetime import datetime
f = open("flight.json", "rb")
j = f.read().decode()
f.close()
js = json.loads(j)
fli = js.get("flights").get(list(js.get("flights").keys())[0])
flino = fli.get("codeShare").get("iataIdent")
takeoff = fli.get("takeoffTimes").get("scheduled")
land = fli.get("landingTimes").get("scheduled")
stat = fli.get("flightStatus")
if stat == "airborne":
    if not fli.get("takeoffTimes").get("actual") == None:
        takeoff = fli.get("takeoffTimes").get("actual")
    else:
        takeoff = fli.get("takeoffTimes").get("estimated")
    land = fli.get("landingTimes").get("estimated")
elif stat == "arrived":
    takeoff = fli.get("takeoffTimes").get("actual")
    land = fli.get("landingTimes").get("actual")
originiata = fli.get("origin").get("iata")
originname = fli.get("origin").get("friendlyLocation")
origintz = fli.get("origin").get("TZ")[1:]
otz = pytz.timezone(origintz)
takeofft = datetime.fromtimestamp(takeoff)
takeofftlocal = takeofft.astimezone(otz)
destiata = fli.get("destination").get("iata")
destname = fli.get("destination").get("friendlyLocation")
desttz = fli.get("destination").get("TZ")[1:]
dtz = pytz.timezone(desttz)
landt = datetime.fromtimestamp(land)
landtlocal = landt.astimezone(dtz)
#airborne
#arrived
msg = fli.get("airline").get("fullName") + " " + flino + " " + originiata + ">" + destiata + " "
if stat == "airborne":
    msg += "途中 "
elif stat == "arrived":
    msg += "到达 "
else:
    msg += "计划 "
msg += takeofftlocal.strftime("%Y-%m-%d") + " "
msg += takeofftlocal.strftime("%H:%M") + "-"
tdt = datetime.strptime(takeofftlocal.strftime("%Y-%m-%d"), "%Y-%m-%d")
ldt = datetime.strptime(landtlocal.strftime("%Y-%m-%d"), "%Y-%m-%d")
diff = (datetime.timestamp(ldt) - datetime.timestamp(tdt))/ 86400
msg += landtlocal.strftime("%H:%M")
if diff > 0:
    msg += "(+" + str(int(diff)) + ") "
elif diff < 0:
    msg += "(" + str(int(diff)) + ") "
else:
    msg += " "
msg += originname + " > " + destname
f2 = open("flight.txt", "w")
f2.write(msg)
f2.close()