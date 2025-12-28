# Kotlin Import Style Guide

## Core Principles

1. **Always use explicit imports** - Import each class/function explicitly
2. **Never use glob/star imports** - No `import foo.bar.*`
3. **Never use inline full classpaths** - Import at the top, use simple names in code
4. **Organize imports alphabetically** within each group

## ✅ Correct Examples

```kotlin
// Good: Explicit imports at the top
import io.pocketbase.PocketBase
import io.pocketbase.models.QueryOptions
import io.pocketbase.models.RecordModel
import kotlinx.serialization.json.JsonObject

fun example() {
    val options = QueryOptions(filter = "id='123'")
    val record = model as? RecordModel
    val json = data as? JsonObject
}
```

## ❌ Incorrect Examples

```kotlin
// Bad: Glob/star imports
import io.pocketbase.models.*

// Bad: Inline full classpath
fun example() {
    val options = io.pocketbase.models.QueryOptions(filter = "id='123'")
    val record = model as? io.pocketbase.models.RecordModel
}
```

## Import Organization

Organize imports into groups (separated by blank lines):

1. **Standard library imports** (kotlin.*)
2. **Third-party library imports** (alphabetical by package)
3. **Project domain imports** (love.bside.app.domain.*)
4. **Project core imports** (love.bside.app.core.*)
5. **Project data imports** (love.bside.app.data.*)
6. **Project UI imports** (love.bside.app.ui.*)

### Example

```kotlin
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import io.pocketbase.PocketBase
import io.pocketbase.models.QueryOptions
import io.pocketbase.models.RecordModel
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject

import love.bside.app.core.Result
import love.bside.app.data.DatabaseCollections
import love.bside.app.data.repository.PocketBaseMessagingRepository
import love.bside.app.domain.repository.MessagingRepository
```

## IDE Configuration

### IntelliJ IDEA / Android Studio

1. Go to **Settings > Editor > Code Style > Kotlin**
2. Under **Imports** tab:
   - Set "Use single name import" for **all** packages
   - Uncheck "Use import with '*'" for all entries
   - Set import layout as described above

### EditorConfig

Add to `.editorconfig`:

```ini
[*.kt]
ij_kotlin_packages_to_use_import_on_demand = unset:.*
ij_kotlin_name_count_to_use_star_import = 999
ij_kotlin_name_count_to_use_star_import_for_members = 999
```

## Common Violations to Fix

### 1. Inline Classpath Usage

```kotlin
// ❌ Before
val options = io.pocketbase.models.QueryOptions(filter = "...")
val record = model as? io.pocketbase.models.RecordModel

// ✅ After
import io.pocketbase.models.QueryOptions
import io.pocketbase.models.RecordModel

val options = QueryOptions(filter = "...")
val record = model as? RecordModel
```

### 2. Star Imports

```kotlin
// ❌ Before
import love.bside.app.domain.models.*

// ✅ After
import love.bside.app.domain.models.Conversation
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.Profile
```

### 3. Unnecessary Qualified Names

```kotlin
// ❌ Before
fun example() {
    val result = kotlin.runCatching {
        // ...
    }
}

// ✅ After (if used frequently)
import kotlin.runCatching

fun example() {
    val result = runCatching {
        // ...
    }
}
```

## Benefits

1. **Better IDE support** - Autocomplete works better with explicit imports
2. **Easier refactoring** - IDE can automatically update imports
3. **Clearer dependencies** - Easy to see what a file depends on
4. **Avoid naming conflicts** - Explicit imports make conflicts obvious
5. **Better code reviews** - Reviewers can see exactly what's being used

## Migration Script

Find and fix inline classpaths:

```bash
# Find files with inline classpaths
grep -r "io\.pocketbase\.models\." --include="*.kt" .

# Find star imports
grep -r "import.*\.\*" --include="*.kt" .
```

## Automated Fixes

Use IntelliJ's built-in tools:

1. **Optimize Imports**: `Ctrl+Alt+O` (Windows/Linux) or `Cmd+Alt+O` (Mac)
2. **Code > Reformat Code** to apply code style
3. **Analyze > Code Cleanup** for batch fixes

## Exceptions

Very rare cases where qualified names are acceptable:

1. **Disambiguation of same-named classes**

   ```kotlin
   // When two classes have the same name from different packages
   val pbModel = io.pocketbase.models.RecordModel()
   val domainModel = love.bside.app.domain.models.RecordModel()
   ```

   But even then, consider using import aliases:

   ```kotlin
   import io.pocketbase.models.RecordModel as PbRecord
   import love.bside.app.domain.models.RecordModel as DomainRecord
   
   val pbModel = PbRecord()
   val domainModel = DomainRecord()
   ```

2. **Platform-specific code blocks**

   ```kotlin
   expect fun platformSpecific()
   
   actual fun platformSpecific() {
       android.util.Log.d("TAG", "Android-specific")
   }
   ```

## Review Checklist

Before submitting a PR, verify:

- [ ] No star imports (`import foo.*`)
- [ ] No inline full classpaths in code
- [ ] All imports are explicit and organized
- [ ] IDE doesn't show any import warnings
- [ ] Run "Optimize Imports" in IDE

## References

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html#imports)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html#s3.3-import-statements) (similar principles)
