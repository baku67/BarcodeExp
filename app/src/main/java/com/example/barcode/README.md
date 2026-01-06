Mode developpeur: taper N° de série 5 fois sur tel
Activer debogage USB

Tunnel Tel -> PC avec adb:
adb reverse --list
adb reverse tcp:8080 tcp:8080


Dub onglet LogCat:
"package:mine level:error" (à coté de l'icone Filter)


Debug LogCat lancé en ligne de commande:
adb devices
adb logcat -c
adb logcat *:E
