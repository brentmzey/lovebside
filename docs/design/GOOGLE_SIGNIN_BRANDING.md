# Google Sign-In Branding Guidelines

## Overview
This document outlines the requirements for implementing "Sign in with Google" following [Google's official branding guidelines](https://developers.google.com/identity/branding-guidelines).

## Current Status
✅ OAuth2 infrastructure exists in codebase (`shared/pocketbase_new_sdk_backup/`)  
✅ **PocketBase** is our auth backend (custom KMM SDK)  
✅ PocketBase supports OAuth2 providers including Google  
⚠️ No Google Sign-In button currently implemented in UI  
📍 Implementation location: `composeApp/src/commonMain/kotlin/love/bside/app/ui/screens/auth/AuthScreen.kt`

## Architecture
We use **PocketBase** as our authentication backend with a custom Kotlin Multiplatform (KMM) SDK. PocketBase has built-in OAuth2 support for Google and other providers. The flow is:

1. User clicks "Sign in with Google" button in our Compose UI
2. App calls PocketBase's `authWithOAuth2()` method via our KMM SDK
3. PocketBase handles the OAuth2 flow and returns auth tokens
4. App stores tokens and authenticates the user

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

### Phase 2: OAuth2 Integration with PocketBase
- [ ] Enable Google OAuth2 provider in PocketBase Admin UI (http://localhost:8090/_/)
  - Navigate to Settings → Auth providers → Google
  - Add OAuth2 Client ID and Client Secret from Google Cloud Console
- [ ] Wire up button to existing `OAuth2` infrastructure in `shared/pocketbase_new_sdk_backup/CollectionService.kt`
- [ ] Call `authWithOAuth2(provider = "google", ...)` via our custom KMM PocketBase SDK
- [ ] Handle redirect URL and authorization code exchange (PocketBase manages this)
- [ ] Store OAuth2 tokens in `AuthDetails` model
- [ ] Update `AuthViewModel.kt` to handle OAuth flow

### Phase 3: Platform-Specific Setup
- [ ] **Android**: Add Google Play Services dependency and configure OAuth client ID
- [ ] **iOS**: Add Google Sign-In SDK and configure URL schemes
- [ ] **Web**: Load Google Identity Services JavaScript library
- [ ] **Desktop**: Use system browser for OAuth flow

## Example Implementation (Compose Multiplatform)

### 1. UI Button Component

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

### 2. OAuth Flow with PocketBase

```kotlin
// In AuthViewModel.kt or similar
class AuthViewModel(
    private val pocketBase: PocketBase
) : ViewModel() {
    
    suspend fun signInWithGoogle() {
        try {
            // Step 1: Get OAuth2 URL from PocketBase
            val authMethods = pocketBase.collection("users")
                .listAuthMethods()
                .getOrThrow()
            
            val googleProvider = authMethods.authProviders
                .find { it.name == "google" }
                ?: throw Exception("Google auth not configured")
            
            // Step 2: Open browser for user consent
            // (Platform-specific implementation)
            val authUrl = googleProvider.authUrl
            val codeVerifier = generateCodeVerifier() // PKCE
            openBrowser("$authUrl&code_verifier=$codeVerifier")
            
            // Step 3: Handle callback with auth code
            // (This would be from deep link or redirect)
            val authCode = waitForAuthCallback()
            
            // Step 4: Complete OAuth2 flow with PocketBase
            val result = pocketBase.collection("users")
                .authWithOAuth2<User>(
                    provider = "google",
                    code = authCode,
                    codeVerifier = codeVerifier,
                    redirectUrl = "myapp://oauth-callback"
                )
                .getOrThrow()
            
            // Step 5: User is now authenticated!
            // PocketBase SDK automatically stores the token
            val user = result.record
            val token = result.token
            
        } catch (e: Exception) {
            // Handle error
        }
    }
}
```

### 3. Integration in AuthScreen

```kotlin
// Add to AuthScreen.kt
Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    
    // Google Sign-In Button
    GoogleSignInButton(
        onClick = { viewModel.signInWithGoogle() },
        enabled = !uiState.isLoading,
        isLoading = uiState.isGoogleAuthLoading
    )
    
    // Divider
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "or",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
    
    // Existing email/password fields
    AuthTextField(
        label = "Email",
        value = uiState.email,
        onValueChange = { /* ... */ }
    )
    // ... rest of form
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

## PocketBase Configuration

### Enabling Google OAuth2 in PocketBase
1. Access PocketBase Admin UI at `http://localhost:8090/_/`
2. Navigate to **Settings** → **Auth providers**
3. Find **Google** and click to configure
4. Enable the provider
5. Add your Google OAuth2 credentials:
   - **Client ID**: From Google Cloud Console
   - **Client Secret**: From Google Cloud Console
6. Set redirect URL: PocketBase will auto-generate this (typically `http://localhost:8090/api/oauth2-redirect`)

### Getting Google OAuth2 Credentials
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google+ API (if not already enabled)
4. Go to **Credentials** → **Create Credentials** → **OAuth 2.0 Client ID**
5. Application type: **Web application**
6. Authorized redirect URIs: Add PocketBase's redirect URL
7. Copy Client ID and Client Secret to PocketBase

## References
- [Google Sign-In Branding Guidelines](https://developers.google.com/identity/branding-guidelines)
- [Google Identity Services](https://developers.google.com/identity/gsi/web/guides/overview)
- [OAuth2 Flow Documentation](https://developers.google.com/identity/protocols/oauth2)
- [PocketBase OAuth2 Documentation](https://pocketbase.io/docs/authentication/)
- [B-Side Custom KMM PocketBase SDK](../../../shared/pocketbase_new_sdk_backup/)

---
**Last Updated**: 2026-01-24  
**Status**: Not yet implemented - ready for development
