# Migration Guide - Adopting Enterprise Features

This guide helps you migrate existing code to use the new enterprise features.

## Table of Contents
1. [Error Handling Migration](#error-handling-migration)
2. [Adding Logging](#adding-logging)
3. [Validation Integration](#validation-integration)
4. [Network Calls Migration](#network-calls-migration)
5. [UI Components Migration](#ui-components-migration)
6. [Configuration Usage](#configuration-usage)

---

## Error Handling Migration

### Before
```kotlin
suspend fun getUser(id: String): User? {
    return try {
        apiClient.getUser(id)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        null
    }
}
```

### After
```kotlin
suspend fun getUser(id: String): Result<User> = retryable {
    apiClient.getUser(id)
}

// Usage
val result = repository.getUser(id)
result.onSuccess { user ->
    updateUI(user)
}.onError { error ->
    logError("Failed to get user", error)
    showError(error.getUserMessage())
}
```

### Benefits
- Type-safe error handling
- No null checking needed
- Automatic retry on network failures
- User-friendly error messages
- Structured error logging

---

## Adding Logging

### Before
```kotlin
class MyRepository {
    suspend fun fetchData() {
        println("Fetching data...")
        val data = api.getData()
        println("Got ${data.size} items")
        return data
    }
}
```

### After
```kotlin
class MyRepository {
    suspend fun fetchData() {
        logDebug("Fetching data...")
        try {
            val data = api.getData()
            logInfo("Successfully fetched ${data.size} items")
            return data
        } catch (e: Exception) {
            logError("Failed to fetch data", e)
            throw e
        }
    }
}
```

### Benefits
- Platform-appropriate logging
- Automatic class name tagging
- Environment-aware verbosity
- Exception stack traces preserved
- Easy to search in logs

---

## Validation Integration

### Before
```kotlin
fun login(email: String, password: String) {
    if (email.isEmpty()) {
        showError("Email is required")
        return
    }
    if (!email.contains("@")) {
        showError("Invalid email")
        return
    }
    if (password.length < 8) {
        showError("Password too short")
        return
    }
    performLogin(email, password)
}
```

### After
```kotlin
fun login(email: String, password: String) {
    val emailValidation = Validators.validateEmail(email)
    val passwordValidation = Validators.validatePassword(password)
    val combined = Validators.combine(emailValidation, passwordValidation)
    
    if (!combined.isValid) {
        showError(combined.getErrorOrNull()?.getUserMessage())
        return
    }
    
    performLogin(email, password)
}
```

### Benefits
- Reusable validation logic
- Consistent error messages
- Chainable validations
- Type-safe error handling
- Easy to extend

---

## Network Calls Migration

### Before
```kotlin
suspend fun loadMatches(): List<Match> {
    var retries = 0
    while (retries < 3) {
        try {
            return apiClient.getMatches()
        } catch (e: Exception) {
            retries++
            if (retries >= 3) throw e
            delay(1000 * retries)
        }
    }
    throw Exception("Failed after retries")
}
```

### After
```kotlin
suspend fun loadMatches(): Result<List<Match>> = retryable {
    apiClient.getMatches()
}
```

### Benefits
- Automatic retry with exponential backoff
- Configurable retry policy
- Smart retry (skips auth errors)
- Exception mapping to AppException
- One-liner implementation

---

## UI Components Migration

### Loading State

#### Before
```kotlin
@Composable
fun MyScreen(isLoading: Boolean, data: Data?) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (data != null) {
        DataContent(data)
    }
}
```

#### After
```kotlin
@Composable
fun MyScreen(state: UiState<Data>) {
    when (state) {
        is UiState.Loading -> LoadingView(message = "Loading data...")
        is UiState.Success -> DataContent(state.data)
        is UiState.Error -> ErrorScreen(
            exception = state.exception,
            onRetry = { retry() }
        )
        is UiState.Empty -> EmptyView(
            message = "No data available",
            actionLabel = "Refresh",
            onAction = { refresh() }
        )
    }
}
```

### Form Fields

#### Before
```kotlin
@Composable
fun EmailField(email: String, onEmailChange: (String) -> Unit) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
}
```

#### After
```kotlin
@Composable
fun EmailField(
    email: String, 
    onEmailChange: (String) -> Unit,
    error: String? = null
) {
    ValidatedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "Email",
        errorMessage = error,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next
    )
}
```

### Buttons with Loading

#### Before
```kotlin
@Composable
fun SubmitButton(onClick: () -> Unit, isLoading: Boolean) {
    Button(
        onClick = onClick,
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
        Text("Submit")
    }
}
```

#### After
```kotlin
@Composable
fun SubmitButton(onClick: () -> Unit, isLoading: Boolean) {
    LoadingButton(
        onClick = onClick,
        text = "Submit",
        isLoading = isLoading,
        modifier = Modifier.fillMaxWidth()
    )
}
```

### Benefits
- Consistent UI/UX across app
- Less boilerplate code
- Material Design 3 styled
- Accessible by default
- Easy to customize

---

## Configuration Usage

### Before
```kotlin
const val API_BASE_URL = "https://api.example.com"
const val TIMEOUT_MS = 30000L
const val MAX_RETRIES = 3

fun createClient() = HttpClient {
    // ...
}
```

### After
```kotlin
fun createClient() = HttpClient {
    val config = appConfig()
    
    install(HttpTimeout) {
        requestTimeoutMillis = config.apiTimeout
    }
    
    if (config.enableLogging) {
        install(Logging) { 
            level = when (config.environment) {
                Environment.DEVELOPMENT -> LogLevel.ALL
                else -> LogLevel.NONE
            }
        }
    }
    
    defaultRequest {
        url(config.apiBaseUrl)
    }
}
```

### Benefits
- Environment-specific configuration
- Feature flags for gradual rollout
- Easy to change without recompilation
- Platform-specific overrides
- Type-safe access

---

## ViewModel Migration

### Before
```kotlin
class MyViewModel : ViewModel() {
    private val _data = MutableStateFlow<Data?>(null)
    val data = _data.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = repository.getData()
                _data.value = result
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

### After
```kotlin
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: AppException) : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
}

class MyViewModel(
    private val useCase: GetDataUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<Data>>(UiState.Loading)
    val state: StateFlow<UiState<Data>> = _state.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            useCase()
                .onSuccess { data ->
                    _state.value = if (data.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(data)
                    }
                }
                .onError { error ->
                    _state.value = UiState.Error(error)
                    logError("Failed to load data", error)
                }
        }
    }
    
    fun retry() = loadData()
}
```

### Benefits
- Single state flow instead of multiple
- Exhaustive when expressions
- Type-safe state handling
- Automatic logging
- Retry functionality

---

## Repository Migration

### Before
```kotlin
class UserRepository(private val api: ApiClient) {
    suspend fun getUser(id: String): User? {
        return try {
            api.getUser(id)
        } catch (e: Exception) {
            null
        }
    }
}
```

### After
```kotlin
class UserRepository(
    private val api: ApiClient,
    private val cache: InMemoryCache<String, User> = InMemoryCache()
) {
    suspend fun getUser(id: String): Result<User> = retryable {
        cache.getOrPut(CacheKeys.userProfile(id)) {
            api.getUser(id)
        }
    }
    
    suspend fun refreshUser(id: String): Result<User> {
        cache.remove(CacheKeys.userProfile(id))
        return getUser(id)
    }
}
```

### Benefits
- Type-safe error handling
- Built-in caching
- Automatic retry
- Refresh capability
- Testable interface

---

## Use Case Migration

### Before
```kotlin
// Business logic mixed in ViewModel or Repository
```

### After
```kotlin
class GetUserProfileUseCase(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(userId: String): Result<UserProfile> {
        logDebug("Getting user profile for: $userId")
        
        // Validation
        if (userId.isBlank()) {
            return Result.Error(
                AppException.Validation.RequiredField("User ID")
            )
        }
        
        // Business logic
        return repository.getUserProfile(userId)
            .map { profile ->
                profile.copy(
                    displayName = profile.displayName.trim(),
                    bio = profile.bio?.take(500) // Max length
                )
            }
    }
}
```

### Benefits
- Single responsibility
- Reusable business logic
- Testable in isolation
- Clear dependencies
- Proper validation placement

---

## Testing Migration

### Before
```kotlin
@Test
fun `test user loading`() = runTest {
    val user = repository.getUser("123")
    assertNotNull(user)
}
```

### After
```kotlin
@Test
fun `getUser returns success when user exists`() = runTest {
    // Given
    val userId = "123"
    val expectedUser = User(userId, "John Doe")
    coEvery { mockApi.getUser(userId) } returns expectedUser
    
    // When
    val result = repository.getUser(userId)
    
    // Then
    assertTrue(result.isSuccess)
    assertEquals(expectedUser, result.getOrNull())
    coVerify(exactly = 1) { mockApi.getUser(userId) }
}

@Test
fun `getUser returns error when API fails`() = runTest {
    // Given
    val userId = "123"
    coEvery { mockApi.getUser(userId) } throws Exception("Network error")
    
    // When
    val result = repository.getUser(userId)
    
    // Then
    assertTrue(result.isError)
    assertTrue(result.getOrNull() == null)
}
```

### Benefits
- More comprehensive testing
- Clear test structure (Given/When/Then)
- Tests both success and failure cases
- Verifies mock interactions
- Type-safe assertions

---

## Gradual Migration Strategy

1. **Phase 1**: Start with new features
   - Use new patterns for all new code
   - Reference existing code as needed
   
2. **Phase 2**: Migrate critical paths
   - Authentication flows
   - Payment processing
   - Data persistence
   
3. **Phase 3**: Migrate high-traffic areas
   - Main screens
   - List views
   - User profile
   
4. **Phase 4**: Migrate remaining code
   - Settings
   - Help screens
   - Less-used features
   
5. **Phase 5**: Cleanup
   - Remove old utility functions
   - Update documentation
   - Refactor duplicates

## Checklist for Each Migration

- [ ] Replace try-catch with Result type
- [ ] Add appropriate logging
- [ ] Implement validation where needed
- [ ] Use retry for network calls
- [ ] Update UI to use new components
- [ ] Add proper error handling
- [ ] Write or update tests
- [ ] Update documentation
- [ ] Review for security issues
- [ ] Test on all platforms

## Common Pitfalls

1. **Mixing patterns**: Don't mix old null-based errors with new Result type
2. **Over-logging**: Don't log in tight loops or frequently called functions
3. **Ignoring errors**: Always handle or propagate Result.Error
4. **Validation placement**: Validate at boundaries (UI, API, use cases)
5. **Cache invalidation**: Remember to invalidate cache when data changes
6. **Platform differences**: Test migration on all target platforms

## Getting Help

- Check `DEVELOPER_GUIDE.md` for usage examples
- Review `PRODUCTIONALIZATION.md` for feature details
- Look at existing migrated code for patterns
- Ask the team for code reviews

## Additional Resources

- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Material Design 3](https://m3.material.io/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Ktor Documentation](https://ktor.io/docs)
- [Koin Documentation](https://insert-koin.io/)
