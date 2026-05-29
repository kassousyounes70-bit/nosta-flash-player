# Nosta Flash Player ⚡

مشغل ألعاب Flash الكلاسيكية للأندرويد — مبني على Ruffle

## الميزات
- استيراد ملفات `.swf` مباشرة
- استيراد ألعاب مضغوطة `.zip` (يفك الضغط تلقائياً)
- استقبال تذكرة دخول من تطبيق NostGames الرئيسي
- واجهة بسيطة وسريعة

## البناء التلقائي
كل push على main يبني APK تلقائياً عبر GitHub Actions.
اذهب إلى **Actions** ← آخر workflow ← **Artifacts** لتحميل APK.

## التكامل مع التطبيق الرئيسي
```kotlin
// في تطبيق NostGames الرئيسي
val ticket = TicketValidator.generate(gameId)
val intent = Intent("com.ncore.nostagames.LAUNCH_GAME").apply {
    setPackage("com.ncore.flashplayer")
    putExtra("ticket", ticket)
    putExtra("game_url", gameUrl)
    putExtra("game_name", gameName)
}
startActivity(intent)
```
