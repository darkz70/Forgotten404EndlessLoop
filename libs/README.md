# libs/

Положи сюда JAR Traveler's Backpack (форк Tiviacz1337).

## Как собрать JAR:

```bash
git clone https://github.com/Tiviacz1337/Travelers-Backpack
cd Travelers-Backpack
git checkout 1.21.11-fabric
./gradlew build
```

Готовый JAR будет в `build/libs/travelersbackpack-10.11.9-fabric.jar`
Скопируй его сюда (в папку `libs/`).

## Потом в build.gradle раскомментируй:

```groovy
modCompileOnly fileTree(dir: 'libs', include: 'travelersbackpack-*.jar')
```
