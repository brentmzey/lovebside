# TypeError Workaround Documentation

## Problem
The CLI crashes with: `TypeError: Cannot read properties of undefined (reading 'startsWith')`

**CRITICAL**: This error crashes the CLI and requires force quit!

## Patterns That Trigger the Error

Based on multiple crash analyses, the error occurs when:
1. Using `find` command with pipes (especially `| head`)
2. Using complex shell redirections with `2>/dev/null`
3. Using `grep` with certain patterns after find
4. Long command chains that produce filtered output
5. **NEW**: Commands with multiple `&&` chains that include pipes or `2>/dev/null`
6. **NEW**: `ls` with glob patterns piped through `wc` in complex chains

### Known Failing Commands:
```bash
# These patterns FAIL and CRASH the CLI:
find <path> -type f -name "*.kt" | head -20
find <path> -type f -name "*.kt" 2>/dev/null
./gradlew <task> 2>&1 | head -50
find <path> | grep -E "(pattern)"

# LATEST FAILURE (Oct 19, 2025):
ls -1 *.md | wc -l && echo "" && ls -1 docs/archive/*.md 2>/dev/null | wc -l && ./gradlew task
# ^ Multiple commands chained with && where some use pipes and 2>/dev/null
```

## Workarounds

### 🚨 GOLDEN RULE: ONE SIMPLE COMMAND AT A TIME
**NEVER** chain multiple commands with `&&` if any of them use:
- Pipes (`|`)
- Output redirection (`2>/dev/null`, `2>&1`)
- Filtering (`head`, `tail`, `grep`)

### Use Simple ls Instead of find
```bash
# Instead of: find shared/src -type f -name "*.kt"
# Use: ls -R shared/src/ (then filter in next step if needed)
ls -la <directory>
ls -R <directory>
```

### Avoid Pipes with head/tail
```bash
# Instead of: command | head -50
# Use: command (let it output naturally)
./gradlew <task> --no-daemon
```

### Break Command Chains Into Separate Calls
```bash
# DANGEROUS (WILL CRASH):
ls *.md | wc -l && ls docs/archive/*.md 2>/dev/null | wc -l

# SAFE (separate bash calls):
# Call 1: ls *.md | wc -l
# Call 2: ls docs/archive/*.md
# Call 3: (count in your head or process output)
```

### Use Multiple Simple Commands Instead of Complex Pipes
```bash
# Instead of: find . -name "*.kt" | grep pattern | head -10
# Step 1: ls -R .
# Step 2: Process results separately
```

### For Gradle Commands
```bash
# Instead of: ./gradlew task 2>&1 | head -50
# Use: ./gradlew task --no-daemon --console=plain
# Or: ./gradlew task --quiet
```

## Safe Command Patterns

These patterns are SAFE and don't trigger the error:
- `ls -la <path>` (single command, no pipes)
- `ls -R <path>` (single command, no pipes)
- `ls -1 *.md` (single command, no pipes)
- `cat <file>`
- `tree <path>` (if installed)
- `./gradlew <task> --no-daemon` (NO pipes, NO &&)
- Simple git commands: `git status`, `git log --no-pager`
- `pwd`, `cd`, `echo`, basic shell commands
- `wc -l <file>` (counting a file, not piped input)

## UNSAFE Patterns (WILL CRASH)

❌ NEVER USE THESE:
- `command1 | wc -l && command2` (pipe + chain)
- `ls *.md 2>/dev/null` (glob + redirect)
- `command1 && command2 2>/dev/null && command3` (multiple redirects)
- `./gradlew task | head -50` (gradle + pipe)
- Any command chain with more than 2 `&&` operators
- Any command with both pipes AND `&&` chains

## Implementation Strategy Going Forward

1. **Always prefer `ls` over `find`** for directory exploration
2. **Never use pipes with `head`, `tail`, or `grep`** in the same command
3. **Break complex commands into multiple simple steps**
4. **Use gradle flags** like `--quiet`, `--console=plain` instead of output redirection
5. **Test commands incrementally** - run simple version first, then add complexity

## Current Session Status

The project has:
- ✅ Compose Multiplatform setup
- ✅ Decompose navigation structure in shared module
- ✅ Basic LoginScreen and MainScreen
- ✅ RootComponent with routing
- ✅ Domain models and data layer

Next steps (using safe commands only):
1. Build the complete UI screens
2. Wire up navigation in composeApp
3. Implement auth flow
4. Add proper UX/feel with animations and theming
5. Test end-to-end functionality

## Testing the Build Safely

To test the build without triggering errors:
```bash
# Simple build test
./gradlew :composeApp:compileKotlinJvm --no-daemon

# If you need to check for errors, run without filters
./gradlew build --no-daemon

# Or use quiet mode
./gradlew :composeApp:compileKotlinJvm --quiet --no-daemon
```
