README

Mode developpeur: taper N° de série 5 fois sur tel
Activer debogage USB

Old Xiaomi : debogage wifi QR code
Samsung : debogage USB

Tunnel Tel -> PC avec adb:
adb reverse --list
adb reverse tcp:8080 tcp:8080   |OU BIEN oneShot si pb:
& "C:\Users\basil\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080

Tunnels multi-devices:
adb devices -l

puis selon les names:
adb -s RFCW712B7VR reverse tcp:8080 tcp:8080
adb -s "adb-775b0860-iwlBS2._adb-tls-connect._tcp" reverse tcp:8080 tcp:8080



Si Databse Inspector affiche frigozen.db (closed): lancer en mode Debug, ou relancer 154 fois


*** LOGCAT:
    package:mine -message:"frameRate=NaN"
    "package:mine level:error" 
    
    Debug LogCat lancé en ligne de commande:
    adb devices
    adb logcat -c
    adb logcat *:E
