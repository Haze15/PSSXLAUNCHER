# PSSXLAUNCHER

Écran d'accueil façon **XMB de la PS3** pour box Android TV (testé pour une Xiaomi Mi Box 4K).
Il est construit à partir d'[OpenXMB](https://github.com/phenom64/OpenXMB) 0.3.0-beta.3 (Syndromatic Ltd, GPLv3) : sons, icônes, police Play, couleurs du mois et de l'heure, vague.

OpenXMB est un programme pour PC (C++23 + Vulkan + SDL2). La puce graphique d'une Mi Box (Mali-450, OpenGL ES 2) ne gère pas Vulkan, donc il ne peut pas tourner tel quel sur la box. PSSXLAUNCHER refait l'interface en HTML/WebGL dans une petite appli Android, avec les ressources d'OpenXMB.

## Ce qu'il fait

- Barre horizontale de catégories : **Paramètres, Photo, Musique, Vidéo, Applis, Jeu, Réseau**, et liste verticale d'éléments, comme sur PS3.
- Applis rangées automatiquement par catégorie (vidéo, musique, jeux, navigateurs…), et toutes dans « Applis ».
- **Vague** animée (shader RetroArch/OpenXMB porté en WebGL 1) et **couleur du fond qui change selon le mois et l'heure**, avec les mêmes tableaux de couleurs qu'OpenXMB.
- **Sons** d'OpenXMB : déplacement, validation, retour, menu, son de démarrage.
- **OK maintenu** sur un élément : menu d'options (ouvrir, mettre en premier, infos sur l'appli, définir comme fond…).
- Paramètres : thème de couleur, fond d'écran (vague ou image), vague affichée ou masquée, sons, son de démarrage, économiseur météo, paramètres de la box.
- Photo : les fonds d'écran fournis, plus les photos de la box et des clés USB (dossiers `Pictures`, `Download`, `DCIM`, `Fonds`).
- Réseau : « Météo en direct » ([haze15.github.io/meteo](https://haze15.github.io/meteo/)) et les navigateurs.
- Heure et météo (Quimper) en haut à droite.
- Se lance au démarrage de la box et quand la télé sort de veille.

## Installer sur la box (Windows / PowerShell)

Il faut activer le débogage réseau de la box (Options pour les développeurs) et avoir `platform-tools` (adb).

```powershell
cd $HOME\platform-tools
.\adb connect 192.168.1.30:5555
.\adb install -r "$HOME\Downloads\PSSXLAUNCHER.apk"
.\adb shell appops set com.haze15.pssxlauncher SYSTEM_ALERT_WINDOW allow
.\adb shell cmd package set-home-activity com.haze15.pssxlauncher/com.haze15.pssxlauncher.Home
.\adb shell am start -n com.haze15.pssxlauncher/.Home
```

Si l'ancien écran d'accueil revient quand on appuie sur Accueil :

```powershell
.\adb shell pm disable-user --user 0 com.google.android.tvlauncher
```

Pour revenir en arrière :

```powershell
.\adb shell pm enable com.google.android.tvlauncher
.\adb uninstall com.haze15.pssxlauncher
```

## Télécommande

| Touche | Action |
|---|---|
| ◀ ▶ | changer de catégorie |
| ▲ ▼ | parcourir la liste |
| OK | ouvrir |
| OK maintenu | menu d'options |
| Retour | fermer / revenir en haut de la liste |

## Organisation du dépôt

```
web/        l'interface : index.html (XMB), wave.js (vague WebGL), colors.js, sons, icônes, police, fonds
android/    l'appli Android : Java (WebView, pont JS, démarrage auto), manifeste, ressources
openxmb/    sources complètes d'OpenXMB 0.3.0-beta.3 d'origine, sans modification
release/    l'APK prêt à installer
build.sh    recompile l'APK sans Android SDK (javac + dx + apktool + uber-apk-signer)
```

## Compiler

```bash
./build.sh      # -> release/PSSXLAUNCHER.apk
```

Il faut Java (JDK 11 ou plus), `curl`, `zip` et `unzip`. Le script télécharge lui-même apktool, dex2jar et uber-apk-signer depuis leurs pages GitHub.
`android/stub/` contient de faux en-têtes de l'API Android : ils servent uniquement à compiler et ne sont pas inclus dans l'APK.

## Licences

- Code : **GPLv3** (voir `LICENSE`), comme OpenXMB.
- Vague : adaptée de RetroArch (GPLv3), voir `LICENSE.RetroArch`.
- Police Play : OFL 1.1 (`web/font/OFL.txt`).
- Sons (`web/snd/`) et icônes (`web/ic/`) : repris d'OpenXMB. Leur origine n'est pas clarifiée par OpenXMB, qui marque les sons comme « unresolved » dans ses manifestes.
- Images de `web/wp/` : fonds d'écran personnels, propriété de leurs auteurs respectifs (Cyberpunk: Edgerunners, Evangelion, Zelda…), non couverts par la GPL.
