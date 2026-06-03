# memoryGameAndroid

## Utiliser ADB
```cd C:\Users\<User>\AppData\Local\Android\Sdk\platform-tools ```

## Lancer notification matin (Déclenche le défi)
```.\adb shell am broadcast -a MORNING_NOTIFICATION -n com.moulis.mamemoire/.NotificationReceiver```
*Note : Relancer cette commande réinitialise l'animation de révélation (pour test).*

## Lancer notification soir (Rappel de saisie)
```.\adb shell am broadcast -a EVENING_NOTIFICATION -n com.moulis.mamemoire/.NotificationReceiver```

## Simuler le mode "Soir" (Débloquer le champ de réponse)
Pas besoin de changer l'heure du téléphone (évite les erreurs de permission) ! Utilise cette commande pour forcer l'interface du soir :
```.\adb shell am start -n com.moulis.mamemoire/.MainActivity --ez FORCE_EVENING true```
*(Relancer l'app normalement pour revenir au mode automatique basé sur l'heure réelle).*

# MDP keystore
aaaaaaaa
