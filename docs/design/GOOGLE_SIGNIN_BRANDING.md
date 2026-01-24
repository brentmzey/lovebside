# Google Sign-In Branding Guidelines

## Overview
This document outlines the requirements for implementing "Sign in with Google" following [Google's official branding guidelines](https://developers.google.com/identity/branding-guidelines).

## Current Status
✅ OAuth2 infrastructure exists in codebase (`shared/pocketbase_new_sdk_backup/`)  
⚠️ No Google Sign-In button currently implemented in UI  
📍 Implementation location: `composeApp/src/commonMain/kotlin/love/bside/app/ui/screens/auth/AuthScreen.kt`

## Requirements

### Button Design

#### Light Background (Recommended)
- **Background**: White (`#FFFFFF`)
- **Text**: Google Sans font, `#1f1f1f` (rgba 0, 0, 0, 0.54)
- **Border**: 1px solid `#747775`
- **Corner Radius**: 2dp (rounded corners)

#### Dark Background (Alternative)
- **Background**: Blue (`#4285F4`)
- **Text**: White, Google Sans font
- **No border**

### Button Text
- ✅ "Sign in with Google" (recommended)
- ✅ "Sign up with Google" 
- ❌ NOT "Sign in with Google+" (Google+ is deprecated)
- ❌ NOT "Login with Google"

### Google "G" Logo
- Download official assets from: https://developers.google.com/identity/branding-guidelines#download
- Must use the multi-color "G" logo
- Logo size: 18x18dp
- Padding: 8dp left margin, centered vertically

### Button Sizing
- **Height**: 40dp (minimum), 48dp (recommended for mobile)
- **Width**: Minimum to fit text, typically 220-240dp
- **Padding**: 12dp horizontal, centered text

### States
1. **Normal**: As described above
2. **Hover**: Slightly darker background (`#f7f7f7` for light, `#3574d9` for dark)
3. **Active/Pressed**: Even darker (`#e8e8e8` for light, `#2c62c4` for dark)
4. **Disabled**: 38% opacity

## Implementation Checklist

### Phase 1: UI Button Component
- [ ] Create `GoogleSignInButton` composable in `composeApp/src/commonMain/kotlin/love/bside/app/ui/components/auth/`
- [ ] Add Google "G" logo asset to resources
- [ ] Implement proper button states (normal, hover, pressed, disabled)
- [ ] Use Google Sans font (or fallback to Roboto if unavailable)
- [ ] Add to `AuthScreen.kt` between landing screen and email/password fields

### Phase 2: OAuth2 Integration
- [ ] Wire up button to existing `OAuth2` infrastructure in `shared/pocketbase_new_sdk_backup/CollectionService.kt`
- [ ] Implement `authWithOAuth2(provider = "google", ...)` flow
- [ ] Handle redirect URL and authorization code exchange
- [ ] Store OAuth2 tokens in `AuthDetails` model
- [ ] Update `AuthViewModel.kt` to handle OAuth flow

### Phase 3: Platform-Specific Setup
- [ ] **Android**: Add Google Play Services dependency and configure OAuth client ID
- [ ] **iOS**: Add Google Sign-In SDK and configure URL schemes
- [ ] **Web**: Load Google Identity Services JavaScript library
- [ ] **Desktop**: Use system browser for OAuth flow

## Example Implementation (Compose Multiplatform)

```kotlin
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, Color(0xFF747775)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1f1f1f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF4285F4)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource("google_g_logo.png"),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
```

## Assets Needed
1. **google_g_logo.png** (18x18dp) - Multi-color "G" logo
2. **google_g_logo_2x.png** (36x36dp) - 2x resolution
3. **google_g_logo_3x.png** (54x54dp) - 3x resolution

Download from: https://developers.google.com/identity/branding-guidelines#download

## Testing Checklist
- [ ] Button renders correctly on light theme
- [ ] Button renders correctly on dark theme (if using dark variant)
- [ ] Hover state works on desktop/web
- [ ] Press state provides visual feedback
- [ ] Loading state shows spinner
- [ ] Disabled state is visually distinct
- [ ] Logo is crisp at all screen densities
- [ ] OAuth flow completes successfully on all platforms

## References
- [Google Sign-In Branding Guidelines](https://developers.google.com/identity/branding-guidelines)
- [Google Identity Services](https://developers.google.com/identity/gsi/web/guides/overview)
- [OAuth2 Flow Documentation](https://developers.google.com/identity/protocols/oauth2)

---
**Last Updated**: 2026-01-24  
**Status**: Not yet implemented - ready for development
