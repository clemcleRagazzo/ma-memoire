# memoryGameAndroid

## Utiliser ADB
```cd C:\Users\<User>\AppData\Local\Android\Sdk\platform-tools ```

## Lancer notification matin
```.\adb shell am broadcast -a MORNING_NOTIFICATION -n com.moulis.mamemoire/.NotificationReceiver```

## Lancer notification soir
```.\adb shell am broadcast -a EVENING_NOTIFICATION -n com.moulis.mamemoire/.NotificationReceiver```