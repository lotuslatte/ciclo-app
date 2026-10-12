# Persisted/serialized models: keep names stable so saved JSON and backups stay readable.
-keep class pe.favio.ciclo.data.** { *; }
-keepattributes *Annotation*, InnerClasses
