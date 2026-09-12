# USB Media Explorer

تطبيق Android لاستكشاف الملفات والوسائط على التخزين الداخلي ووحدات USB/OTG وبطاقات SD. يقرأ التطبيق الملفات من مصدرها عبر `File` أو Storage Access Framework، ويعمل دون Backend أو Telemetry أو اتصال شبكي أثناء التشغيل.

## الحالة الحالية

المشروع في مرحلة تطوير نشطة. آخر حالة تحقق معروفة:

- الفحص الساكن ومحلل الملفات نظيفان، وملفات CI بصياغة صحيحة.
- التحقق المحلي يحتاج Android SDK 36؛ لا يُعد APK ناجحًا قبل نجاح Workflow **Build APK**.
- نسخة Debug تُرفع إلى GitHub Actions Artifact للمطورين، بينما Release تحتاج أسرار التوقيع.

المسارات الرئيسية الموجودة هي:

- تصفح التخزين، Breadcrumb، الفرز، البحث، التصفية، RTL، وتحديد عدة عناصر.
- نسخ ونقل وحذف وإعادة تسمية وإنشاء وضغط وفك ضغط مع staging وjournal وحماية Zip-Slip.
- معاينات حقيقية للصور والفيديو وأغلفة المجلدات مع تحميل كسول وكاش محدود.
- مشغل Media3/ExoPlayer مع قائمة تشغيل، استئناف، سرعة، نسبة عرض، ترجمة ومسارات صوت.
- إيماءات Player: السحب الأفقي للـseek والرأسي للصوت/الإضاءة مع threshold وحساسية منفصلة.
- اكتشاف mount/unmount وإعادة توصيل الوحدات وتحديث حالة الصلاحيات.
- واجهة إنجليزية وعربية مع دعم RTL وLight/Dark وDynamic Color.

آخر إصلاحات مهمة تشمل استعادة وجهة الأب من Navigation back stack بدل إعادة تحميل URI جذري اصطناعي، تسلسل تحديثات التخزين، حفظ آخر bookmark خارج ViewModel scope، إزالة مسار All Files Access غير المدعوم، وتصحيح اتجاه وحساسية إيماءات الفيديو.

## المتطلبات

| الأداة | الإصدار |
|---|---|
| JDK | 17 |
| Gradle Wrapper | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.1.20 |
| compileSdk / targetSdk | 36 |
| minSdk | 24 |
| Android SDK المحلي | `platforms;android-36` و`build-tools;36.0.0` |

لتشغيل الاختبارات الآلية على المحاكي، يلزم أيضًا صور API 29 وAPI 35 كما هو موضح في [docs/BUILD.md](docs/BUILD.md).

## البناء المحلي

اضبط Android SDK أولًا. مثال Linux/macOS:

```bash
sdkmanager --licenses
sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools"
printf 'sdk.dir=%s\n' "$ANDROID_SDK_ROOT" > local.properties
```

إذا كان SDK في مسار مختلف، استخدم ذلك المسار في `local.properties`. لا تضف الملف إلى Git.

ثم شغّل:

```bash
./gradlew --version
./gradlew :app:testDebugUnitTest --no-daemon --console=plain
./gradlew :app:lintDebug --no-daemon --console=plain
./gradlew :app:assembleDebug --no-daemon --console=plain
```

الناتج:

```text
app/build/outputs/apk/debug/app-debug.apk
```

لتشغيل اختبار Android المتصل:

```bash
./gradlew :app:connectedDebugAndroidTest --no-daemon --console=plain
```

المحاكي لا يحاكي USB OTG حقيقيًا؛ اختبر mount/unmount وSAF والتشغيل من وحدة فعلية على هاتف يدعم OTG.

## GitHub Actions وAPK Artifacts

يعمل Workflow التحقق في `.github/workflows/verify.yml` على Pull Requests وعلى `main` وفروع `arena/**`، ويشغّل:

1. اختبارات JVM.
2. Android Lint.
3. Debug Build.
4. Instrumented Smoke على API 29 و35.

يعمل `.github/workflows/build-apk.yml` عند push إلى `main` أو tag يبدأ بـ`v` أو تشغيل يدوي. عند نجاح Debug Build والاختبارات وLint، يرفع:

```text
USB-Media-Explorer-debug-apk
```

كـGitHub Actions Artifact لمدة 30 يومًا. هذا الـArtifact هو نسخة Debug للمطورين، وليس Release عامًا.

للحصول عليه، افتح تبويب **Actions** واختر تشغيلًا ناجحًا من Workflow **Build APK**، ثم نزّل
`USB-Media-Explorer-debug-apk`. لا يظهر Artifact إذا فشل البناء أو الاختبارات أو Lint.

نسخة Release تحتاج أسرار توقيع GitHub التالية:

- `USBMEDIA_KEYSTORE_B64`
- `USBMEDIA_STORE_PASSWORD`
- `USBMEDIA_KEY_ALIAS`
- `USBMEDIA_KEY_PASSWORD`

على tags، يفشل Workflow صراحة عند غياب أسرار التوقيع. بعد توفر الأسرار يبني Release بـR8، يتحقق من الشهادة باستخدام `apksigner`، ويرفع `app-release.apk` كـArtifact وينشره ضمن Release. التشغيل اليدوي أو push إلى `main` ينتج Artifact Debug فقط ولا يحتاج أسرار التوقيع.

## البنية التقنية

```text
Compose UI + Material 3
        |
Screens + ViewModels + Navigation Compose
        |
DocRepository
   +----+----+
FileDocProvider  SafDocProvider
        |
Internal / USB / OTG / SD storage
```

- `AppContainer`: dependency graph واحد على مستوى العملية.
- `DocRepository`: واجهة موحدة بين `file://` و`content://`.
- `VolumeRepository`: تعداد الوحدات وحالة mount والصلاحيات.
- `BrowseViewModel`: حالة القائمة، الذاكرة المؤقتة للمجلدات، التحديد، والفرز.
- `PlayerViewModel`: Media3، playlist، bookmarks، المسارات، والأخطاء.
- `FileOpsManager` و`FileOpsEngine`: عمليات طويلة مع foreground service وjournal.
- `JsonStore`: مخازن صغيرة versioned مع backup واستعادة من corruption.
- `ThumbnailRepository`: كاش محدود وتوليد معاينات متوازٍ بحدود مناسبة لوحدات USB البطيئة.

التفاصيل الموسعة موجودة في [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## الصلاحيات والأمان

- التخزين الخارجي الحديث يستخدم SAF tree grants.
- صلاحيات الصور والفيديو تستخدم فقط عند الحاجة للتخزين الداخلي عبر المسارات.
- لا يعلن التطبيق `MANAGE_EXTERNAL_STORAGE` ولا يعتمد على All Files Access.
- لا يوجد تصريح `INTERNET` أو Backend أو Analytics.
- عمليات النقل المرحلية لا تحذف المصدر قبل اكتمال الوجهة والتحقق منها.
- فك الضغط يطبق حدودًا على عدد العناصر والعمق والطول والحجم والمساحة الحرة.
- مفاتيح التوقيع لا تدخل المستودع؛ التوقيع يعتمد على أسرار خارجية فقط.
- بيانات المفضلة والملفات الأخيرة ومواضع التشغيل محلية. لا تُرسل خارج الجهاز.
- لا تشارك Crash Report أو URI أو أسماء الملفات إلا نتيجة إجراء صريح من المستخدم.

راجع [PRIVACY.md](PRIVACY.md) و[keystore/README.md](keystore/README.md).

## الاختبارات

اختبارات JVM الحالية تغطي منطقًا نقيًا، منها:

- `PlaybackFailureTest`: تصنيف أخطاء Media3 مع حالات decoder/network adversarial.
- `GestureMathTest`: اتجاه seek، حساسية السحب، وthreshold.
- `NavigationRoutesTest`: ترميز مسارات Navigation.
- `JsonStoreMigrationTest`, `OpsIntegrityTest`, `OpsSafetyTest`.
- `CoverRulesTest`, `FormattersTest`, `SearchBudgetTest`, `RenameAndSortTest`.
- `QaContractTest` باستخدام Fake provider وFake player وTest clock.

يوجد أيضًا `ApplicationSmokeTest` في `androidTest` لإقلاع التطبيق على المحاكي.

في بيئة لا تحتوي Android SDK ستتوقف Gradle برسالة `SDK location not found`; هذا فشل إعداد بيئة،
وليس نتيجة اختبار ناجح أو فاشل. استخدم GitHub Actions أو ثبّت `platforms;android-36` محليًا.

## الاختبار اليدوي المهم

قبل الإصدار، اختبر على جهاز فعلي:

- USB OTG وSD مع SAF grant ثم سحب الصلاحية وإعادة منحها.
- Storage Root → Folder → Back عدة مرات.
- فصل الوحدة أثناء التصفح، توليد thumbnail، التشغيل، والنسخ.
- Android 11 و13 و14/15 مع صلاحيات صور وفيديو وصوت مختلفة.
- ملفات فيديو تالفة أو غير مدعومة وترجمات خارجية.
- مجلدات تحتوي 1,000 و10,000 ملف ووحدة USB بطيئة.
- RTL وTalkBack وحجم خط 200% وشاشات هاتف/tablet.
- قتل العملية أثناء عملية نقل ثم فحص journal وتنظيف staging.

## توقيع Release محليًا

أنشئ مفتاحًا خارج المستودع، ثم اضبط متغيرات البيئة التالية:

```bash
export USBMEDIA_KEYSTORE_PATH=/secure/path/usbmedia.p12
export USBMEDIA_STORE_PASSWORD='...'
export USBMEDIA_KEY_ALIAS='...'
export USBMEDIA_KEY_PASSWORD='...'
./gradlew :app:assembleRelease --no-daemon --console=plain
```

تحقق من الناتج:

```bash
apksigner verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

لا تحفظ ملف keystore أو كلمات المرور في Git. راجع [keystore/README.md](keystore/README.md) قبل تدوير مفتاح التوقيع.

## الترخيص والخصوصية

- الترخيص: [Apache License 2.0](LICENSE).
- سياسة الخصوصية: [PRIVACY.md](PRIVACY.md).
- التطبيق مصمم ليعمل Offline ولا يحتوي خدمة مزامنة أو تحليلات أو نقل بيانات إلى خادم.

## مستندات إضافية

- [بناء وتشغيل](docs/BUILD.md)
- [البنية المعمارية](docs/ARCHITECTURE.md)
- [معايير القبول](docs/ACCEPTANCE_CRITERIA.md)
- [مصفوفة الاختبار](docs/TEST_MATRIX.md)
- [مراجعة SAF](docs/SAF_REVIEW.md)
- [مراجعة عمليات الملفات](docs/OPS_REVIEW.md)
- [مراجعة التوقيع](docs/SIGNING_REVIEW.md)
