# Crashlytics 스택트레이스를 mapping 으로 복원하려면 줄 번호가 남아 있어야 한다.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager 가 끌어오는 Room 2.6 이하의 consumer rule 은 생성자를 지정하지 않아
# R8 full mode 에서 WorkDatabase_Impl.<init>() 이 제거된다. Room 2.7 이상으로 오르면 지워도 된다.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# onnxruntime 은 consumer rule 이 없고 LiteRT 는 @UsedByReflection 만 유지해서,
# JNI 가 이름으로 찾는 패키지 전체를 keep 한다.
-keep class ai.onnxruntime.** { *; }
-keep class org.tensorflow.lite.** { *; }
