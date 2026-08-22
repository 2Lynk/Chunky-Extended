# OneBlock Multi-Loader Mod — Copilot Context

## Goal
Set up a single Minecraft mod that builds for **Fabric**, **Forge**, and **NeoForge** using the **Architectury** multi-loader framework, with shared logic in a `common` subproject and per-loader entrypoints in `fabric/`, `forge/`, `neoforge/`.

---

## Target Versions
| Property | Value |
|---|---|
| Minecraft | `26.1.2` |
| Java | `25` |
| Fabric Loader | `0.19.2` |
| Fabric API | `0.146.1+26.1.2` |
| Forge | `26.1.2-64.0.5` |
| NeoForge | `26.1.2.30-beta` |
| Architectury API | `19.0.1` |
| Architectury Loom | `1.14.473` |
| Architectury Plugin | `3.4-SNAPSHOT` |
| Package group | `lynk.oneblock` |
| Mod ID | `oneblock` |

---

## Project Structure
```
OneBlock-Arch/
├── settings.gradle          # includes common, fabric, forge, neoforge
├── build.gradle             # root: architectury-plugin + loom + shadow applied to all subprojects
├── gradle.properties        # all versions/properties above
├── gradle/wrapper/          # gradle-wrapper.properties (Gradle 9.4.1)
├── gradlew / gradlew.bat    # ⚠️ NOT YET CREATED — wrapper JAR needs to be bootstrapped
│
├── common/
│   ├── build.gradle         # architectury { common('fabric','forge','neoforge') }
│   └── src/main/java/lynk/oneblock/
│       └── OneBlockMod.java # shared DeferredRegister<Item>, init(), MOD_ID
│
├── fabric/
│   ├── build.gradle         # Architectury Fabric module (shadow + remap pattern)
│   └── src/main/
│       ├── java/lynk/oneblock/
│       │   ├── OneBlock.java              # implements ModInitializer, calls OneBlockMod.init()
│       │   ├── OneBlockDataGenerator.java # DataGeneratorEntrypoint
│       │   └── mixin/ExampleMixin.java    # example mixin
│       └── resources/
│           ├── fabric.mod.json            # entrypoint: lynk.oneblock.OneBlock
│           └── oneblock.mixins.json
│
├── forge/
│   ├── build.gradle         # Architectury Forge module (shadow + remap pattern)
│   └── src/main/
│       ├── java/lynk/oneblock/
│       │   ├── OneBlockForge.java  # ⚠️ NEEDS CREATING — @Mod class calling OneBlockMod.init()
│       │   └── Config.java        # currently at com/example/examplemod — needs moving
│       └── resources/META-INF/
│           ├── mods.toml           # still uses examplemod — needs updating
│           └── MANIFEST.MF
│
└── neoforge/
    ├── build.gradle         # Architectury NeoForge module (shadow + remap pattern)
    └── src/main/
        ├── java/lynk/oneblock/
        │   ├── OneBlock.java       # @Mod class, full DeferredRegister setup — GOOD
        │   ├── OneBlockClient.java # client-side setup
        │   └── Config.java        # ModConfigSpec — GOOD
        └── resources/META-INF/
            └── neoforge.mods.toml  # uses ${mod_id}, ${neo_version} placeholders — GOOD
```

---

## What's Done ✅
- `gradle.properties` — all versions set for 26.1.2
- `build.gradle` (root) — Architectury loom `1.14.473`, shadow plugin
- `fabric/build.gradle`, `forge/build.gradle`, `neoforge/build.gradle` — all converted from standalone templates to Architectury multi-loader pattern
- `common/src/main/java/lynk/oneblock/OneBlockMod.java` — shared init, package renamed
- `fabric/` Java sources — all at `lynk.oneblock`, entrypoints correct
- `neoforge/` Java sources — all at `lynk.oneblock`, fully fleshed out from template
- `gradle/wrapper/gradle-wrapper.properties` — pointing to Gradle 9.4.1

---

## What Still Needs Doing ⚠️

### 1. Gradle wrapper scripts
`gradlew`, `gradlew.bat`, and `gradle-wrapper.jar` do not exist yet.
Copy them from `/tmp/oneblock_inspect/neoforge_26_1_2/` (already extracted, uses Gradle 9.4.1):
```fish
cp /tmp/oneblock_inspect/neoforge_26_1_2/gradlew /home/niels/Documents/Projects/OneBlock-code/OneBlock-Arch/gradlew
cp /tmp/oneblock_inspect/neoforge_26_1_2/gradlew.bat /home/niels/Documents/Projects/OneBlock-code/OneBlock-Arch/gradlew.bat
cp /tmp/oneblock_inspect/neoforge_26_1_2/gradle/wrapper/gradle-wrapper.jar /home/niels/Documents/Projects/OneBlock-code/OneBlock-Arch/gradle/wrapper/gradle-wrapper.jar
chmod +x /home/niels/Documents/Projects/OneBlock-code/OneBlock-Arch/gradlew
```

### 2. Move common source file to correct package directory
File is still on disk at `common/src/main/java/dev/oneblock/OneBlockMod.java`.
Needs to move to `common/src/main/java/lynk/oneblock/OneBlockMod.java`:
```fish
mkdir -p common/src/main/java/lynk/oneblock
mv common/src/main/java/dev/oneblock/OneBlockMod.java common/src/main/java/lynk/oneblock/OneBlockMod.java
rm -rf common/src/main/java/dev
```

### 3. Forge Java sources — replace examplemod with oneblock
- Delete `forge/src/main/java/com/example/examplemod/ExampleMod.java` and `Config.java`
- Create `forge/src/main/java/lynk/oneblock/OneBlockForge.java`:
```java
package lynk.oneblock;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OneBlockMod.MOD_ID)
public class OneBlockForge {
    public OneBlockForge(FMLJavaModLoadingContext context) {
        OneBlockMod.init();
    }
}
```
- Create `forge/src/main/java/lynk/oneblock/Config.java` (port from `com.example.examplemod.Config` using `ForgeConfigSpec`)

### 4. Fix forge/src/main/resources/META-INF/mods.toml
Replace all `examplemod` hardcoded values with `${mod_id}` / `${version}` / `${mod_name}` / `${mod_description}` placeholders (matching the `processResources` expand block in `forge/build.gradle`).

### 5. Build validation
```fish
cd /home/niels/Documents/Projects/OneBlock-code/OneBlock-Arch
./gradlew :common:build :fabric:build :forge:build :neoforge:build
```
Fix any remaining dependency resolution or compilation errors.
