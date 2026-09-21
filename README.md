# RP Dry Fish App v3

RP Dry Fish business app starter/production source.

### Included
- Home dashboard
- New Dry Fish bill
- Rate per 40 KG
- Total bora with automatic ₹200/bora
- Labour, paid and due
- Bill number/date and purchase period
- Customer-wise due summary
- Purchase KG report
- PDF bill generation
- PDF/WhatsApp sharing through Android Sharesheet
- JSON backup/share
- Restore bills from JSON backup
- Local persistent storage on the phone
- Assam Heritage inspired red/cream/gold branding

### Important bill rule
The visible bill does not show a separate rate-calculation section. The calculation is performed internally:
`kg / 40 × rate + bora × ₹200 + labour`.

### Build
Open the folder in Android Studio and build the debug APK or signed release AAB/APK.

This environment does not have the Android SDK/Gradle installation required to compile an APK, so the ZIP is the source project rather than a claimed prebuilt APK.
