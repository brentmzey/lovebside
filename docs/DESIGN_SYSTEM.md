# bside.app Design System
**Last Updated**: October 20, 2025  
**Status**: In Progress - Extracting from Figma

---

## 🎨 Current Color Palette

### Primary Colors (Deep Purple/Magenta - from Figma)
```kotlin
Primary: #4B164C (Deep Purple) - Main brand color, sophisticated & romantic
  - Use: Main actions, branding, highlights, headers
  - Container: #8D4E88 (mid-tone purple)
  - Dark: #2A0D2B (very dark purple)
```

### Secondary Colors (Lavender/Purple Tints)
```kotlin
Secondary: #523F67 (Purple-gray) - Secondary elements
  - Use: Text, icons, secondary actions
  - Container: #FAECF8 (light pink-lavender) - backgrounds, surfaces
  - Dark: #4A154B (dark variant)
```

### Logo Accent (Mint Tile)
```kotlin
Accent: #A7D8D1 (Mint teal from the bside_logo.png background)
  - Use: Light-mode background, highlight badges, onboarding hero blocks
  - Dark Variant: #5F8680 (muted teal for dark surfaces)
  - Keeps the UI aligned with the app icon on Android/iOS/desktop installers
```

### Tertiary Colors (Soft Accents)
```kotlin
Tertiary: #F8E6F6 (Pink tint) - Highlights and accents
  - Container: #EFEDF1 (neutral lavender) - surface variants
```

### Semantic Colors
```kotlin
Success: #4CAF50 (Green) - matches, positive actions
Warning: #FF9800 (Orange) - cautions
Error: #F44336 (Red) - errors, destructive actions
```

### Neutrals (From Figma)
```kotlin
Gray 50: #FFFBF0 (warm off-white)
Gray 100: #EFEEEE (light gray)
Gray 200: #E8E1E8 (purple-tinted gray)
Gray 700: #C0BDC4 (mid gray)
Gray 900: #555072 (dark purple-gray)
```

### Dark Mode
```kotlin
Background: #1A0D1B (deep purple-black)
Surface: #2A0D2B (dark purple surface)
Surface Variant: #4B164C (brand primary)
```

---

## 📐 Typography

### Font Family
- **Display**: System default / Sans-serif
- **Body**: System default
- **Note**: Figma may specify custom fonts - TODO: Extract from PDF

### Type Scale (Material 3)
```
Display Large: 57sp, Regular
Display Medium: 45sp, Regular
Display Small: 36sp, Regular

Headline Large: 32sp, Regular
Headline Medium: 28sp, Regular
Headline Small: 24sp, Regular

Title Large: 22sp, Medium
Title Medium: 16sp, Medium
Title Small: 14sp, Medium

Body Large: 16sp, Regular
Body Medium: 14sp, Regular
Body Small: 12sp, Regular

Label Large: 14sp, Medium
Label Medium: 12sp, Medium
Label Small: 11sp, Medium
```

---

## 🎯 Component Specifications

### Buttons
- **Height**: 56dp (comfortable touch target)
- **Corner Radius**: 16dp (modern, friendly)
- **Min Width**: 88dp
- **Padding**: Horizontal 24dp, Vertical 16dp
- **Elevation**: 
  - Default: 2dp
  - Pressed: 8dp
  - Disabled: 0dp

### Cards
- **Corner Radius**: 16-20dp (varies by use case)
- **Elevation**: 2-4dp
- **Padding**: 16-24dp
- **Image Aspect Ratio**: 
  - Profile cards: 3:4 (portrait)
  - Hero images: 16:9

### Input Fields
- **Height**: 56dp
- **Corner Radius**: 24dp (rounded pill shape)
- **Border Width**: 2dp (focused), 1dp (unfocused)
- **Padding**: Horizontal 16dp

### Spacing System
```
XXS: 4dp
XS: 8dp
S: 12dp
M: 16dp (base unit)
L: 24dp
XL: 32dp
XXL: 48dp
```

---

## 🖼️ Layouts from Figma

### Discovery Screen
- **Layout**: Grid (2 columns on mobile, 3-4 on tablet)
- **Card Size**: Full width with 16dp gaps
- **Profile Card Elements**:
  - Photo (top, 60% of card height)
  - Name + Age (bold, prominent)
  - Location (icon + text)
  - Bio preview (2 lines max)
  - Match score badge (top-right overlay)

### Messages Screen  
- **Layout**: List (full width items)
- **Conversation Card Elements**:
  - Avatar (56dp circle, left)
  - Name + timestamp (top row)
  - Last message preview (bottom row)
  - Unread badge (right, if applicable)
  - Elevated background for unread

### Chat Detail Screen
- **Layout**: Column (messages stacked)
- **Message Bubbles**:
  - Sent: Right-aligned, primary color, max 75% width
  - Received: Left-aligned, surface color, max 75% width
  - Corner Radius: 20dp (with one small corner 4dp)
  - Padding: 12-16dp
  - Timestamp: Below bubble, small gray text

### Profile Detail Screen
- **Layout**: Scroll (LazyColumn)
- **Sections**:
  1. Hero photo (400dp height, curved bottom corners)
  2. Basic info (name, age, location, occupation)
  3. About section (bio)
  4. Shared interests (chips)
  5. Compatibility breakdown (progress bars)
  6. Conversation starters
  7. Action buttons (fixed bottom bar)

---

## 🎭 Colors Extracted from Figma PDF

### Found Color Codes
Based on PDF analysis, these colors appear in the design:
```
#22222f - Likely dark background/text
#ffffff - White (backgrounds, text on color)
#eff0ff - Very light purple (possible container)
#555999 - Medium purple (possible secondary)
#033333 - Very dark cyan (possible accent)
```

**Status**: Need to verify these match our current implementation

---

## 📊 Current vs Figma Comparison

### ✅ Implemented from Figma
- Deep purple/magenta primary color (#4B164C)
- Lavender background tint (#FAECF8)
- Purple-gray neutrals
- Larger corner radius (24-32dp for extraLarge)
- Modern card elevation (2-8dp)
- Refined spacing (12dp grid base)

### ⚠️ To Verify
- Exact primary color (#E91E63 vs Figma spec)
- Secondary color usage
- Custom font family (if any)
- Specific gradients or overlays
- Icon styles and sizes

### 🔍 To Extract from Figma
- [ ] Exact color palette (RGB values)
- [ ] Custom font names and weights
- [ ] Logo usage guidelines
- [ ] Image filters/overlays
- [ ] Animation specifications
- [ ] Micro-interaction details

---

## 🎨 Theming Guidelines

### Light Mode (Default)
- Background: Soft gray (#FAFAFA) - not harsh white
- Surface: Pure white with subtle shadows
- Primary actions: Vibrant pink (#E91E63)
- Text: Near-black (#212121) for readability

### Dark Mode
- Background: True black (#121212) for OLED
- Surface: Elevated dark (#1E1E1E)
- Primary: Lighter tint (#F48FB1) for contrast
- Text: Off-white (#E1E1E1) to reduce eye strain

---

## 🚀 Design System Status

### Implemented ✅
- **Figma color palette** (deep purple #4B164C + lavender tints)
- Complete color system (light + dark mode)
- Typography scale (Material 3)
- Spacing system (4dp grid)
- Component styles (buttons, cards, inputs)
- Rounded shapes (8-32dp corner radius)
- Card elevation system (2-8dp)

### In Progress 🔄
- Extracting exact Figma colors
- Custom font integration (if needed)
- Logo usage in all screens
- Animations and transitions

### TODO 📋
- [ ] Add custom fonts if specified in Figma
- [ ] Document animation timings
- [ ] Create style guide for illustrations
- [ ] Define photo aspect ratios and crops
- [ ] Extract icon styles from Figma screens

---

## 💡 Design Principles

### Dating App Aesthetic
1. **Warm & Inviting**: Pink primary creates friendly, romantic feel
2. **Thoughtful Depth**: Purple secondary represents intellectual connection (Proust)
3. **Modern & Clean**: Generous spacing, rounded corners, clear hierarchy
4. **Accessible**: High contrast, 48dp+ touch targets, screen reader support
5. **Delightful**: Smooth animations, responsive feedback, micro-interactions

### UI/UX Goals
- **Instant Clarity**: Users know what to do immediately
- **Emotional Connection**: Design supports meaningful interactions
- **Trust & Safety**: Professional, polished, secure feeling
- **Platform Native**: Respects iOS, Android, Desktop conventions
- **Fast & Smooth**: 60fps animations, <200ms response times

---

---

## 🎨 Design Import Options

### Screenshots (Quickest)

Take screenshots of:

- Color Styles Panel - All your colors with hex codes
- Text Styles Panel - Typography with font specs
- Key Screens/Frames - Main app screens
- Component Examples - Buttons, cards, inputs, etc.
- Spacing/Layout Grid - If you have defined spacing tokens

### Manual Spec Sheet

Create a quick spec document with:

```
Colors:
- Primary: #XXXXXX
- Secondary: #XXXXXX
- Background: #XXXXXX
...

Typography:
- Heading 1: Font, Size, Weight
- Body: Font, Size, Weight
...

Screens:
- Home
- Profile
- Settings
...
```

### Alternative: Export & Share

You can:

- Export frames as images (File → Export)
- Screenshot the design system (colors, typography)
- Share component examples

Then recreate everything pixel-perfect in Compose Multiplatform!

---

## 🏗️ Architecture

```
UI Layer (Compose)
    ↓
ViewModel (shared/)
    ↓
Repository (shared/data/)
    ↓
Ktor Internal API (server/)
    ↓
PocketBase
```

Your UI will only talk to ViewModels, which talk to the internal API. Clean separation! ✅

---

## 📝 Next Steps

1. **Open Figma PDF** in design viewer to extract:
   - Exact color codes
   - Font specifications
   - Layout measurements
   - Component details

2. **Update Theme.kt** with verified colors

3. **Add custom fonts** if specified

4. **Create logo variants** for different contexts

5. **Document animations** (duration, easing, triggers)

---

**Note**: This is a living document. Update as we extract more details from Figma and refine the design system.
